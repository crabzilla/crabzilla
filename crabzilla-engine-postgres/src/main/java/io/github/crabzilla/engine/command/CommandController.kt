package io.github.crabzilla.engine.command

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.LockingException
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.CommandValidator
import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.engine.assertAffectedRows
import io.github.crabzilla.engine.projector.EventsProjector
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.command.FutureCommandHandler
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class CommandController<S : State, C : Command, E : Event>(
  private val vertx: Vertx,
  private val config: CommandControllerConfig<S, C, E>,
  private val pgPool: PgPool,
  private val jsonSerDer: JsonSerDer,
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
    const val DEFAULT_STATS_INTERVAL = 10_000L
  }

  private val commandsOk = AtomicLong(0)
  private val commandsFailures = AtomicLong(0)
  private val commandHandler = commandHandler()

  init {
    vertx.setPeriodic(DEFAULT_STATS_INTERVAL) { publishMetrics() }
    publishMetrics()
  }

  private fun publishMetrics() {
    val metric = JsonObject()
      .put("controllerId", config.name)
      .put("successes", commandsOk.get())
      .put("failures", commandsFailures.get())
    vertx.eventBus().publish("crabzilla.command-controllers", metric)
  }

  fun handle(metadata: CommandMetadata, command: C): Future<CommandSession<S, E>> {
    fun validateCommands(): Future<Void> {
      val validator: CommandValidator<C>? = config.commandValidator
      return if (validator != null) {
        val validationErrors = validator.validate(command)
        return if (validationErrors.isNotEmpty()) {
          Future.failedFuture(ValidationException(validationErrors))
        } else {
          Future.succeededFuture()
        }
      } else {
        Future.succeededFuture()
      }
    }
    fun getSnapshot(conn: SqlConnection): Future<Snapshot<S>?> {
      fun snapshot(rowSet: RowSet<Row>): Snapshot<S>? {
        return if (rowSet.size() == 0) {
          log.debug("Not locked (1)")
          null
        } else {
          val r = rowSet.first()
          val version = r.getInteger("version")
          val locked = r.getBoolean("locked")
          val id = r.getLong("id")
          if (!advisoryLockOption || locked) {
            log.debug("Locked {} {}", id, metadata.stateId.id)
            val stateAsJson = JsonObject(r.getValue("json_content").toString())
            val state = jsonSerDer.stateFromJson(stateAsJson.toString()) as S
            Snapshot(state, version)
          } else {
            throw (LockingException("Not locked $id ${metadata.stateId.id}"))
          }
        }
      }
      return conn
        .preparedQuery(SQL_LOCK_SNAPSHOT)
        .execute(Tuple.of(metadata.stateId.id))
        .map { pgRow -> snapshot(pgRow) }
    }
    fun appendCommand(conn: SqlConnection): Future<Void> {
      if (!saveCommandOption) {
        return Future.succeededFuture(null)
      }
      val cmdAsJson = jsonSerDer.toJson(command)
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(
        metadata.commandId.id,
        JsonObject(cmdAsJson)
      )
      return conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .mapEmpty()
    }
    fun appendEvents(conn: SqlConnection, initialVersion: Int, events: List<E>): Future<AppendedEvents<E>> {
      fun appendEvent(event: E, version: Int, causationId: CausationId): Future<AppendedEvent<E>> {
        val eventAsJson = jsonSerDer.toJson(event)
        log.debug("Will append event {} as {}", event, eventAsJson)
        val eventId = UuidCreator.getTimeOrdered()
        val params = Tuple.of(
          causationId.id,
          metadata.correlationId.id,
          config.name,
          metadata.stateId.id,
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
      val version = AtomicInteger(initialVersion)
      val eventsAdded = mutableListOf<AppendedEvent<E>>()
      return events.fold(
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
    fun updateSnapshot(conn: SqlConnection, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void> {
      val newSTateAsJson = jsonSerDer.toJson(newState)
      log.debug("Will append snapshot {} to version {}", newSTateAsJson, resultingVersion)
      return if (originalVersion == 0) {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.stateId.id,
          config.name
        )
        conn.preparedQuery(SQL_INSERT_VERSION).execute(params).mapEmpty()
      } else {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.stateId.id,
          originalVersion
        )
        conn
          .preparedQuery(SQL_UPDATE_VERSION)
          .execute(params)
          .transform { it.assertAffectedRows(1) }
      }
    }
    fun projectEvents(conn: SqlConnection, appendedEvents: AppendedEvents<E>): Future<Void> {
      if (eventsProjector == null) {
        return Future.succeededFuture()
      }
      log.debug("Will project events")
      val initialFuture = Future.succeededFuture<Void>()
      return appendedEvents.events.fold(
        initialFuture
      ) { currentFuture: Future<Void>, appendedEvent ->
        currentFuture.compose {
          val eventMetadata = EventMetadata(
            config.name,
            metadata.stateId,
            EventId(appendedEvent.eventId),
            CorrelationId(metadata.commandId.id),
            CausationId(appendedEvent.causationId),
            appendedEvent.sequence
          )
          eventsProjector.project(conn, appendedEvent.event, eventMetadata)
        }
      }.mapEmpty()
    }

    val snapshotResult: AtomicReference<Snapshot<S>?> = AtomicReference(null)
    val sessionResult: AtomicReference<CommandSession<S, E>> = AtomicReference(null)
    val appendedEventsResult: AtomicReference<AppendedEvents<E>> = AtomicReference(null)

    return pgPool.withTransaction { conn ->
      validateCommands()
        .compose {
          log.debug("Command validated")
          getSnapshot(conn)
        }.compose { snapshot ->
          snapshotResult.set(snapshot)
          log.debug("Got snapshot {}", snapshot)
          commandHandler.invoke(command, snapshot?.state)
        }.compose { session ->
          sessionResult.set(session)
          log.debug("Command handled {}", session.toSessionData())
          appendCommand(conn)
        }.compose {
          log.debug("Command appended")
          val originalVersion = snapshotResult.get()?.version ?: 0
          appendEvents(conn, originalVersion, sessionResult.get().appliedEvents())
        }.compose {
          log.debug("Events appended")
          appendedEventsResult.set(it)
          projectEvents(conn, it)
        }.compose {
          val originalVersion = snapshotResult.get()?.version ?: 0
          updateSnapshot(
            conn,
            originalVersion, appendedEventsResult.get().resultingVersion, sessionResult.get().currentState
          )
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

  private fun commandHandler(): (command: C, state: S?) -> Future<CommandSession<S, E>> {
    return when (val handler: CommandHandlerApi<S, C, E> = config.commandHandlerFactory.invoke()) {
      is CommandHandler<S, C, E> -> { command, state ->
        try {
          val session = handler.handleCommand(command, state)
          Future.succeededFuture(session)
        } catch (e: Throwable) {
          Future.failedFuture(e)
        }
      }
      is FutureCommandHandler<S, C, E> -> { command, state ->
        handler.handleCommand(command, state)
      }
      else -> throw IllegalArgumentException("Unknown command handler")
    }
  }
}
