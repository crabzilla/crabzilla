package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.command.CommandException
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.command.EventStore
import io.github.crabzilla.stack.foldLeft
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class PgcEventStore<A : DomainState, C : Command, E : DomainEvent>(
  private val config: CommandControllerConfig<A, C, E>,
  private val pgPool: PgPool,
  private val json: Json,
  private val saveCommandOption: Boolean = true,
  private val optimisticLockOption: Boolean = true, // only use false if you have a singleton command handler verticle
  private val eventsProjectorApi: EventsProjector? = null,
) : EventStore<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(EventStore::class.java)
    const val SQL_LOCK_VERSION =
      """ SELECT version 
          FROM SNAPSHOTS 
          WHERE ar_id = $1 
            AND ar_type = $2   
          FOR SHARE"""
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ INSERT INTO events (causation_id, correlation_id, ar_name, ar_id, event_payload, version)
          VALUES ($1, $2, $3, $4, $5, $6) returning id, causation_id, sequence"""
    const val SQL_INSERT_VERSION =
      """ INSERT INTO snapshots (version, json_content, ar_id, ar_type)
          VALUES ($1, $2, $3, $4)"""
    const val SQL_UPDATE_VERSION =
      """ UPDATE snapshots 
          SET version = $1, json_content = $2 
          WHERE ar_id = $3 
          AND version = $4"""
  }

  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    fun lockSnapshotIfVersionMatches(conn: SqlConnection): Future<Void> {
      if (!optimisticLockOption) {
        return Future.succeededFuture()
      }
      val promise0 = Promise.promise<Void>()
      conn.preparedQuery(SQL_LOCK_VERSION)
        .execute(Tuple.of(metadata.domainStateId.id, config.name))
        .onSuccess { rowSet ->
          val currentVersion = if (rowSet.size() == 0) {
            0
          } else {
            rowSet.first().getInteger("version")
          }
          log.debug("Got current version: {}", currentVersion)
          if (currentVersion != session.originalVersion) {
            val message = "The current version [$currentVersion] should be [${session.originalVersion}]"
            promise0.fail(CommandException.OptimisticLockingException(message))
          } else {
            promise0.complete()
          }
        }
        .onFailure {
          promise0.fail(it)
        }
      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Void> {
      if (!saveCommandOption) {
        return Future.succeededFuture(null)
      }
      val cmdAsJson = command.toJson(json)
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(
        metadata.commandId.id,
        JsonObject(cmdAsJson)
      )
      return conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .mapEmpty()
    }

    fun appendEvents(conn: SqlConnection): Future<AppendedEvents<E>> {
      fun appendEvent(event: E, version: Int, causationId: CausationId): Future<AppendedEvent<E>> {
        val eventAsJson = event.toJson(json)
        log.debug("Will append event {} as {}", event, eventAsJson)
        val params = Tuple.of(
          causationId.id,
          metadata.correlationId.id,
          config.name,
          metadata.domainStateId.id,
          JsonObject(eventAsJson),
          version
        )
        return conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .map { rs: RowSet<Row> ->
            AppendedEvent(
              event,
              rs.first().getUUID("causation_id"),
              rs.first().getLong("sequence"),
              rs.first().getUUID("id")
            )
          }
      }

      val initialFuture = Future.succeededFuture<AppendedEvent<E>?>(null)
      val version = AtomicInteger(session.originalVersion)
      val eventsAdded = mutableListOf<AppendedEvent<E>>()

      return foldLeft(
        session.appliedEvents().iterator(), initialFuture
      ) {
        currentFuture: Future<AppendedEvent<E>?>,
        event: E,
        ->
        currentFuture.compose { appendedEvent: AppendedEvent<E>? ->
          val causationId = if (eventsAdded.size == 0) metadata.commandId.id else appendedEvent!!.eventId
          appendEvent(event, version.incrementAndGet(), CausationId(causationId))
            .onSuccess { eventsAdded.add(it) }
        }
      }.map { AppendedEvents(eventsAdded, version.get()) }
    }

    fun updateVersion(conn: SqlConnection, resultingVersion: Int): Future<Void> {
      val newSTateAsJson = session.currentState.toJson(json)
      log.debug("Will append snapshot {}", newSTateAsJson)
      return if (session.originalVersion == 0) {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.domainStateId.id,
          config.name
        )
        conn.preparedQuery(SQL_INSERT_VERSION).execute(params).mapEmpty()
      } else {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.domainStateId.id,
          session.originalVersion
        )
        conn.preparedQuery(SQL_UPDATE_VERSION).execute(params).mapEmpty()
      }
    }

    fun projectEvents(conn: SqlConnection, appendedEvents: AppendedEvents<E>): Future<Void> {
      if (eventsProjectorApi == null) {
        return Future.succeededFuture()
      }
      val initialFuture = Future.succeededFuture<Void>()
      return foldLeft(
        appendedEvents.events.iterator(), initialFuture
      ) { currentFuture: Future<Void>, appendedEvent ->
        currentFuture.compose {
          val eventMetadata = EventMetadata(
            config.name,
            metadata.domainStateId,
            EventId(appendedEvent.eventId),
            CorrelationId(metadata.commandId.id),
            CausationId(appendedEvent.causationId),
            appendedEvent.sequence
          )
          eventsProjectorApi.project(conn, appendedEvent.event, eventMetadata)
        }
      }.mapEmpty()
    }

    return pgPool.withTransaction { conn ->
      lockSnapshotIfVersionMatches(conn)
        .compose { appendCommand(conn) }
        .compose {
          appendEvents(conn)
            .compose { eventsAppended ->
              projectEvents(conn, eventsAppended)
                .compose { updateVersion(conn, eventsAppended.resultingVersion) }
            }
        }
    }.mapEmpty()
  }

  private data class AppendedEvent<E>(val event: E, val causationId: UUID, val sequence: Long, val eventId: UUID)

  private data class AppendedEvents<E>(val events: List<AppendedEvent<E>>, val resultingVersion: Int)
}
