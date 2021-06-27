package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CommandException
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventStore
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction

class PgcEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val pgPool: PgPool,
  private val saveCommandOption: Boolean = false,
  private val eventsProjector: PgcEventsProjector<E>? = null,
) : EventStore<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventStore::class.java)
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

  private fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
    var result = identity
    while (iterator.hasNext()) {
      val next = iterator.next()
      result = bf.apply(result, next)
    }
    return result
  }

  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    fun lockSnapshotIfVersionMatches(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      conn.preparedQuery(SQL_LOCK_VERSION)
        .execute(Tuple.of(metadata.aggregateRootId.id, config.name))
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
      val p = Promise.promise<Void>()
      val cmdAsJson = command.toJson(config.json)
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(
        metadata.commandId.id,
        JsonObject(cmdAsJson)
      )
      conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .onFailure { p.fail(it) }
        .onSuccess {
          p.complete()
        }
      return p.future()
    }

    fun appendEvents(conn: SqlConnection): Future<AppendedEvents<E>> {
      fun appendEvent(event: E, version: Int, causationId: CausationId): Future<AppendedEvent<E>> {
        val aePromise = Promise.promise<AppendedEvent<E>>()
        val eventAsJson = event.toJson(config.json)
        log.debug("Will append event {} as {}", event, eventAsJson)
        val params = Tuple.of(
          causationId.id,
          metadata.correlationId.id,
          config.name,
          metadata.aggregateRootId.id,
          JsonObject(eventAsJson),
          version
        )
        conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .onFailure { aePromise.fail(it) }
          .onSuccess { rs ->
            val appendedEvent = AppendedEvent(
              event,
              rs.first().getUUID("causation_id"),
              rs.first().getLong("sequence"),
              rs.first().getUUID("id")
            )
            aePromise.complete(appendedEvent)
            log.debug("Append event ok {}", appendedEvent)
          }
        return aePromise.future()
      }

      val aesPromise = Promise.promise<AppendedEvents<E>>()
      val initialFuture = Future.succeededFuture<AppendedEvent<E>?>(null)
      val version = AtomicInteger(session.originalVersion)
      val eventsAdded = mutableListOf<AppendedEvent<E>>()
      foldLeft(
        session.appliedEvents().iterator(), initialFuture,
        {
          currentFuture: Future<AppendedEvent<E>?>,
          event: E,
          ->
          currentFuture.compose { appendedEvent: AppendedEvent<E>? ->
            val causationId = if (eventsAdded.size == 0) metadata.commandId.id else appendedEvent!!.eventId
            appendEvent(event, version.incrementAndGet(), CausationId(causationId))
              .onSuccess { eventsAdded.add(it) }
          }
        }
      ).onComplete {
        if (it.failed()) {
          aesPromise.fail(it.cause())
        } else {
          aesPromise.complete(AppendedEvents(eventsAdded, version.get()))
        }
      }
      return aesPromise.future()
    }

    fun updateVersion(conn: SqlConnection, resultingVersion: Int): Future<Void> {
      val promise = Promise.promise<Void>()
      val newSTateAsJson = session.currentState.toJson(config.json)
      log.debug("Will append snapshot {}", newSTateAsJson)
      if (session.originalVersion == 0) {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id,
          config.name
        )
        conn.preparedQuery(SQL_INSERT_VERSION).execute(params)
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      } else {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id,
          session.originalVersion
        )
        conn.preparedQuery(SQL_UPDATE_VERSION).execute(params)
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      }
      return promise.future()
    }

    fun projectEvents(conn: SqlConnection, appendedEvents: AppendedEvents<E>): Future<Void> {
      if (eventsProjector == null) {
        return Future.succeededFuture()
      }
      val projectorPromise = Promise.promise<Void>()
      val initialFuture = Future.succeededFuture<Void>()
      foldLeft(
        appendedEvents.events.iterator(), initialFuture,
        { currentFuture: Future<Void>, appendedEvent ->
          currentFuture.compose {
            val eventMetadata = EventMetadata(
              config.name,
              metadata.aggregateRootId,
              EventId(appendedEvent.eventId),
              CorrelationId(metadata.commandId.id),
              CausationId(appendedEvent.causationId),
              appendedEvent.sequence
            )
            eventsProjector.project(conn, appendedEvent.event, eventMetadata)
          }
        }
      ).onComplete {
        if (it.failed()) {
          projectorPromise.fail(it.cause())
        } else {
          projectorPromise.complete()
        }
      }
      return projectorPromise.future()
    }

    val promise = Promise.promise<Void>()

    pgPool.withTransaction { conn ->
      lockSnapshotIfVersionMatches(conn)
        .compose { appendCommand(conn) }
        .compose {
          appendEvents(conn)
            .compose { eventsAppended ->
              projectEvents(conn, eventsAppended)
                .compose { updateVersion(conn, eventsAppended.resultingVersion) }
            }
        }
    }.onSuccess {
      log.debug("Events successfully committed")
      promise.complete()
    }
      .onFailure {
        promise.fail(it)
      }

    return promise.future()
  }

  data class AppendedEvent<E>(val event: E, val causationId: UUID, val sequence: Long, val eventId: UUID)

  data class AppendedEvents<E>(val events: List<AppendedEvent<E>>, val resultingVersion: Int)
}
