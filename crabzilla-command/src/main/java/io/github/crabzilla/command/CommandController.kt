package io.github.crabzilla.command

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.command.internal.CommandHandlerWrapper.wrap
import io.github.crabzilla.command.internal.SnapshotRepository
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.EventTopics
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.LockingException
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.CommandValidator
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class CommandController<S : State, C : Command, E : Event>(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val jsonSerDer: JsonSerDer,
  private val config: CommandControllerConfig<S, C, E>,
  private val snapshotRepository: SnapshotRepository<S, E>,
  private val eventsProjector: EventsProjector? = null,
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    private const val NOTIFICATIONS_INTERVAL = 3000L
    private const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    private const val SQL_APPEND_EVENT =
      """ INSERT 
            INTO events (event_type, causation_id, correlation_id, state_type, state_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8) returning sequence"""
  }

  private val notificationsByStateType = HashSet<String>()

  init {
    vertx.setPeriodic(NOTIFICATIONS_INTERVAL) {
      notificationsByStateType.forEach { stateType ->
        pgPool
          .query("NOTIFY " + EventTopics.CRABZILLA_ROOT_TOPIC.name.lowercase() + ", '" + stateType + "'")
          .execute()
      }
      notificationsByStateType.clear()
    }
  }

  private val commandHandler: (command: C, state: S?) -> Future<CommandSession<S, E>> =
    wrap(config.commandHandlerFactory.invoke())

  fun handle(metadata: CommandMetadata, command: C): Future<CommandSession<S, E>> {
    fun validateCommands(): Future<Void> {
      val validator: CommandValidator<C> = config.commandValidator ?: return succeededFuture()
      val validationErrors = validator.validate(command)
      return if (validationErrors.isNotEmpty()) {
        failedFuture(ValidationException(validationErrors))
      } else {
        succeededFuture()
      }
    }
    fun lock(conn: SqlConnection, lockId: Int): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(config.name.hashCode(), lockId))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            succeededFuture()
          } else {
            failedFuture(LockingException("Can't be locked ${metadata.stateId.id}"))
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
      var version = initialVersion
      val eventIds = events.map { UuidCreator.getTimeOrdered() }
      val tuples: List<Tuple> = events.mapIndexed { index, event ->
        val causationId: UUID = if (index == 0) metadata.commandId.id else eventIds[(index - 1)]
        val eventAsJson = jsonSerDer.toJson(event)
        val eventId = eventIds[index]
        val jsonObject = JsonObject(eventAsJson)
        val type = jsonObject.getString("type")
        jsonObject.remove("type")
        Tuple.of(
          type,
          causationId,
          metadata.correlationId.id,
          config.name,
          metadata.stateId.id,
          jsonObject,
          ++version,
          eventId
        )
      }
      val appendedEventList = mutableListOf<AppendedEvent<E>>()
      return conn.preparedQuery(SQL_APPEND_EVENT)
        .executeBatch(tuples)
        .onSuccess { rowSet ->
          var rs: RowSet<Row>? = rowSet
          tuples.mapIndexed { index, _ ->
            val sequence = rs!!.iterator().next().getLong("sequence")
            val correlationId = tuples[index].getUUID(2)
            val eventId = tuples[index].getUUID(7)
            appendedEventList.add(AppendedEvent(events[index], correlationId, sequence, eventId))
            rs = rs!!.next()
          }
        }.map {
          AppendedEvents(appendedEventList, version)
        }
    }
    fun projectEvents(
      conn: SqlConnection,
      appendedEvents: AppendedEvents<E>,
      projector: EventsProjector,
    ): Future<Void> {
      log.debug("Will project {} events", appendedEvents.events.size)
      val initialFuture = succeededFuture<Void>()
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
          log.debug("Events appended {}", appendedEvents.events.toString())
          if (eventsProjector != null) {
            projectEvents(conn, appendedEvents, eventsProjector)
              .onSuccess {
                log.debug("Events projected")
              }.map { triple }
          } else {
            succeededFuture(triple)
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
          notificationsByStateType.add(config.name)
          succeededFuture(session)
        }
    }
  }

  private data class AppendedEvent<E>(val event: E, val causationId: UUID, val sequence: Long, val eventId: UUID)

  private data class AppendedEvents<E>(val events: List<AppendedEvent<E>>, val resultingVersion: Int)
}
