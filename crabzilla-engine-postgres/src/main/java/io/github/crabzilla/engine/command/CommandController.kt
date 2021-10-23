package io.github.crabzilla.engine.command

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.LockingException
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.CommandValidator
import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.engine.projector.EventsProjector
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.command.CommandHandlerWrapper.wrap
import io.github.crabzilla.stack.command.CommandMetadata
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

class CommandController<S : State, C : Command, E : Event>(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val jsonSerDer: JsonSerDer,
  private val config: CommandControllerConfig<S, C, E>,
  private val snapshotRepository: SnapshotRepository<S, E>,
  private val eventsProjector: EventsProjector? = null
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    private const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    private const val SQL_APPEND_EVENT =
      """ INSERT INTO events (causation_id, correlation_id, ar_name, ar_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7) returning sequence"""
    private const val DEFAULT_STATS_INTERVAL = 10_000L
  }

  private val commandsOk = AtomicLong(0)
  private val commandsFailures = AtomicLong(0)
  private val commandHandler: (command: C, state: S?) -> Future<CommandSession<S, E>> =
    wrap(config.commandHandlerFactory.invoke())

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
    fun lock(conn: SqlConnection, lockId: Int): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(config.name.hashCode(), lockId))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            Future.succeededFuture()
          } else {
            Future.failedFuture(LockingException("Not locked ${metadata.stateId.id}"))
          }
        }
    }
    fun appendCommand(conn: SqlConnection): Future<Void> {
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
    fun projectEvents(
      conn: SqlConnection,
      appendedEvents: AppendedEvents<E>,
      projector: EventsProjector
    ): Future<Void> {
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
          projector.project(conn, appendedEvent.event, eventMetadata)
        }
      }.mapEmpty()
    }

    return pgPool.withTransaction { conn ->
      validateCommands()
        .compose {
          log.debug("Command validated")
          lock(conn, metadata.stateId.id.hashCode())
        }.compose {
          log.debug("State locked")
          snapshotRepository.get(conn, metadata.stateId.id)
        }.compose { snapshot ->
          log.debug("Got snapshot {}", snapshot)
          commandHandler.invoke(command, snapshot?.state)
            .map { Pair(snapshot, it) }
        }.compose { pair ->
          val (_, session) = pair
          log.debug("Command handled {}", session.toSessionData())
          appendCommand(conn)
            .map { pair }
        }.compose { pair ->
          val (snapshot, session) = pair
          log.debug("Command appended")
          val originalVersion = snapshot?.version ?: 0
          appendEvents(conn, originalVersion, session.appliedEvents())
            .map { Triple(snapshot, session, it) }
        }.compose { triple ->
          val (_, _, appendedEvents) = triple
          log.debug("Events appended")
          if (eventsProjector != null) {
            projectEvents(conn, appendedEvents, eventsProjector)
              .onSuccess {
                log.debug("Events projected")
              }.map { triple }
          } else {
            Future.succeededFuture(triple)
          }
        }.compose { triple ->
          val (snapshot, session, appendedEvents) = triple
          val originalVersion = snapshot?.version ?: 0
          snapshotRepository.upsert(
            conn, metadata.stateId.id, originalVersion,
            appendedEvents.resultingVersion, session.currentState
          ).map { triple }
        }.compose { triple ->
          val (_, session, _) = triple
          Future.succeededFuture(session)
        }.onSuccess {
          commandsOk.incrementAndGet()
        }.onFailure {
          commandsFailures.incrementAndGet()
        }
    }
  }

  private data class AppendedEvent<E>(val event: E, val causationId: UUID, val sequence: Long, val eventId: UUID)

  private data class AppendedEvents<E>(val events: List<AppendedEvent<E>>, val resultingVersion: Int)
}
