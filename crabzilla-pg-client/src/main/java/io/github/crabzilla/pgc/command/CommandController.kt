package io.github.crabzilla.pgc.command

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.CommandException
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandHandlerApi
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.command.FutureCommandHandler
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class CommandController<A : DomainState, C : Command, E : DomainEvent>(
  private val vertx: Vertx,
  private val config: CommandControllerConfig<A, C, E>,
  private val pgPool: PgPool,
  private val json: Json,
  private val saveCommandOption: Boolean = true,
  private val advisoryLockOption: Boolean = true,
  private val eventsProjector: EventsProjector? = null
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    const val SQL_LOCK_SNAPSHOT =
      """ SELECT version, json_content, pg_try_advisory_xact_lock(s.id) as locked, s.id as id
          FROM snapshots s
         WHERE ar_id = $1 """
//           AND ar_type = $2"""
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ INSERT INTO events (causation_id, correlation_id, ar_name, ar_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7) returning sequence"""
    const val SQL_INSERT_VERSION =
      """ INSERT INTO snapshots (version, json_content, ar_id, ar_type)
          VALUES ($1, $2, $3, $4)"""
    const val SQL_UPDATE_VERSION =
      """ UPDATE snapshots 
          SET version = $1, json_content = $2 
          WHERE ar_id = $3 
           AND version = $4"""
//    AND ar_type = $4
  }

  private val commandsOk = AtomicLong(0)
  private val commandsFailures = AtomicLong(0)

  private val commandHandler = commandHandler()

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>> {
    fun validateCommands(): Future<Void> {
      val validator: CommandValidator<C>? = config.commandValidator
      return if (validator != null) {
        val validationErrors = validator.validate(command)
        return if (validationErrors.isNotEmpty()) {
          Future.failedFuture(CommandException.ValidationException(validationErrors))
        } else {
          Future.succeededFuture()
        }
      } else {
        Future.succeededFuture()
      }
    }
    fun getSnapshot(conn: SqlConnection): Future<Snapshot<A>?> {
      fun snapshot(rowSet: RowSet<Row>): Snapshot<A>? {
        return if (rowSet.size() == 0) {
          log.debug("Not locked (1)")
          null
        } else {
          val r = rowSet.first()
          val version = r.getInteger("version")
          val locked = r.getBoolean("locked")
          val id = r.getLong("id")
          if (!advisoryLockOption || locked) {
            log.debug("Locked {} {}", id, metadata.domainStateId.id)
            val stateAsJson = JsonObject(r.getValue("json_content").toString())
            val state = DomainState.fromJson<A>(json, stateAsJson.toString())
            Snapshot(state, version)
          } else {
            throw (CommandException.LockingException("Not locked $id ${metadata.domainStateId.id}"))
          }
        }
      }
      return conn
        .preparedQuery(SQL_LOCK_SNAPSHOT)
        .execute(Tuple.of(metadata.domainStateId.id))
        .map { pgRow -> snapshot(pgRow) }
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
    fun appendEvents(conn: SqlConnection, session: StatefulSession<A, E>): Future<AppendedEvents<E>> {
      fun appendEvent(event: E, version: Int, causationId: CausationId): Future<AppendedEvent<E>> {
        val eventAsJson = event.toJson(json)
        log.debug("Will append event {} as {}", event, eventAsJson)
        val eventId = UuidCreator.getTimeOrdered()
        val params = Tuple.of(
          causationId.id,
          metadata.correlationId.id,
          config.name,
          metadata.domainStateId.id,
          JsonObject(eventAsJson),
          version,
          eventId
        )
        return conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .map { rs: RowSet<Row> ->
            AppendedEvent(
              event,
              causationId.id,
              rs.first().getLong("sequence"),
              eventId
            )
          }
      }
      val initialFuture = Future.succeededFuture<AppendedEvent<E>?>(null)
      val version = AtomicInteger(session.originalVersion)
      val eventsAdded = mutableListOf<AppendedEvent<E>>()
      return session.appliedEvents().fold(
        initialFuture
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
    fun updateSnapshot(conn: SqlConnection, resultingVersion: Int, session: StatefulSession<A, E>): Future<Void> {
      val newSTateAsJson = session.currentState.toJson(json)
      log.debug("Will append snapshot {} to version {}", newSTateAsJson, resultingVersion)
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
        conn
          .preparedQuery(SQL_UPDATE_VERSION)
          .execute(params)
          .transform { ar: AsyncResult<RowSet<Row>> ->
            if (ar.succeeded()) {
              if (ar.result().rowCount() == 1) {
                Future.succeededFuture()
              } else {
                Future.failedFuture("Expected 1 update but it was ${ar.result().size()}")
              }
            } else {
              Future.failedFuture("When updating version")
            }
          }
      }
    }
    fun projectEvents(conn: SqlConnection, appendedEvents: AppendedEvents<E>): Future<Void> {
      if (eventsProjector == null) {
        return Future.succeededFuture()
      }
      val initialFuture = Future.succeededFuture<Void>()
      return appendedEvents.events.fold(
        initialFuture
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
          eventsProjector.project(conn, appendedEvent.event, eventMetadata)
        }
      }.mapEmpty()
    }

    vertx.setPeriodic(10_000) {
      val metric = JsonObject()
        .put("controllerId", config.name)
        .put("successes", commandsOk.get())
        .put("failures", commandsFailures.get())
      vertx.eventBus().publish("crabzilla.command-controllers", metric)
    }

    val sessionResult: AtomicReference<StatefulSession<A, E>> = AtomicReference(null)
    val appendedEventsResult: AtomicReference<AppendedEvents<E>> = AtomicReference(null)

    return pgPool.withTransaction { conn ->
      validateCommands()
        .compose {
          log.debug("Command validated")
          getSnapshot(conn)
        }.compose { snapshot ->
          log.debug("Got snapshot {}", snapshot)
          commandHandler.invoke(command, snapshot)
        }.compose { session ->
          log.debug("Command handled {}", session.toSessionData())
          sessionResult.set(session)
          appendCommand(conn)
        }.compose {
          log.debug("Command appended")
          appendEvents(conn, sessionResult.get())
        }.compose {
          log.debug("Events appended")
          appendedEventsResult.set(it)
          projectEvents(conn, it)
        }.compose {
          if (eventsProjector != null) {
            log.debug("Events projected")
          }
          updateSnapshot(conn, appendedEventsResult.get().resultingVersion, sessionResult.get())
        }.compose {
          Future.succeededFuture(sessionResult.get())
        }.onSuccess {
          commandsOk.incrementAndGet()
        }.onFailure {
          commandsFailures.incrementAndGet()
        }
    }
  }

  private data class AppendedEvent<E>(val event: E, val causationId: UUID, val sequence: Long, val eventId: UUID)

  private data class AppendedEvents<E>(val events: List<AppendedEvent<E>>, val resultingVersion: Int)

  private fun commandHandler(): (command: C, snapshot: Snapshot<A>?) -> Future<StatefulSession<A, E>> {
    return when (val handler: CommandHandlerApi<A, C, E> = config.commandHandlerFactory.invoke()) {
      is CommandHandler<A, C, E> -> { command, snapshot ->
        try {
          val session = handler.handleCommand(command, snapshot)
          Future.succeededFuture(session)
        } catch (e: Throwable) {
          Future.failedFuture(e)
        }
      }
      is FutureCommandHandler<A, C, E> -> { command, snapshot ->
        handler.handleCommand(command, snapshot)
      }
      else -> throw IllegalArgumentException("Unknown command handler")
    }
  }
}
