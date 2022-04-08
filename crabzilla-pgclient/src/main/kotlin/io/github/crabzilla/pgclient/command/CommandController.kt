package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.LockingException
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.pgclient.EventTopics
import io.github.crabzilla.pgclient.EventsProjector
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
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class CommandController<out S: Any, C: Any, E: Any>(
  vertx: Vertx,
  private val pgPool: PgPool,
  private val json: Json,
  private val config: CommandControllerConfig<S, C, E>,
  private val eventsProjector: EventsProjector? = null
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    private const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val GET_EVENTS_BY_ID =
      """
      SELECT event_type, event_payload, version
      FROM events
      WHERE state_id = $1
      ORDER BY sequence
    """
    private const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    private const val SQL_APPEND_EVENT =
      """ INSERT 
            INTO events (event_type, causation_id, correlation_id, state_type, state_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8) returning sequence"""
    private const val DEFAULT_NOTIFICATION_INTERVAL = 3000L

  }

  private val stateSerialName = config.stateClass.simpleName!!
  private val commandSerDer = PolymorphicSerializer(config.commandClass)
  private val eventSerDer = PolymorphicSerializer(config.eventClass)
  private val notificationsByStateType = HashSet<String>()

  init {
    log.info("Starting CommandController for $stateSerialName")
    notificationsByStateType.add(stateSerialName)
    vertx.setPeriodic(DEFAULT_NOTIFICATION_INTERVAL) {
      notifyPg()
    }
    notifyPg()
  }

  private fun notifyPg() {
    notificationsByStateType.forEach { stateType ->
      pgPool
        .preparedQuery("NOTIFY " + EventTopics.STATE_TOPIC.name.lowercase() + ", '$stateType'")
        .execute()
    }
    notificationsByStateType.clear()
  }

  private val commandHandler: CommandHandler<S, C, E> = config.commandHandlerFactory.invoke()

  fun handle(metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    return pgPool.withTransaction { conn: SqlConnection ->
      handle(conn, metadata, command)
    }
  }

  fun compose(f: (SqlConnection) -> Future<CommandSideEffect>): Future<CommandSideEffect> {
    return pgPool.withTransaction(f)
  }

  fun handle(conn: SqlConnection, metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    return validate(command)
      .compose {
        log.debug("Command validated")
        lock(conn, metadata.stateId.hashCode(), metadata)
          .compose {
            log.debug("State locked")
            getSnapshot(conn, metadata.stateId)
          }.compose { snapshot ->
            log.debug("Got snapshot {}", snapshot)
            succeededFuture(Pair(snapshot, commandHandler.handleCommand(command, snapshot?.state)))
          }.compose { pair ->
            val (_, session) = pair
            log.debug("Command handled {}", session.toSessionData())
            appendCommand(conn, command, metadata)
              .map { pair }
          }.compose { pair ->
            val (snapshot, session) = pair
            log.debug("Command appended")
            val originalVersion = snapshot?.version ?: 0
            appendEvents(conn, originalVersion, session.appliedEvents(), metadata)
              .map { Triple(snapshot, session, it) }
          }.compose { triple ->
            val (_, _, commandSideEffect) = triple
            log.debug("Events appended {}", commandSideEffect.toString())
            if (eventsProjector != null) {
              projectEvents(conn, commandSideEffect, eventsProjector, metadata)
                .onSuccess {
                  log.debug("Events projected")
                }.map { commandSideEffect }
            } else {
              log.debug("EventsProjector is null, skipping projecting events")
              succeededFuture(commandSideEffect)
            }
          }.compose {
            notificationsByStateType.add(stateSerialName)
            val result = CommandSideEffect(it.appendedEvents, it.resultingVersion)
            succeededFuture(result)
          }
      }
  }

  private fun validate(command: C): Future<Void> {
    if (config.commandValidator != null) {
      val errors = config.commandValidator!!.validate(command)
      if (errors.isNotEmpty()) {
        return failedFuture(ValidationException(errors))
      }
    }
    return succeededFuture()
  }

  private fun lock(conn: SqlConnection, lockId: Int, metadata: CommandMetadata): Future<Void> {
    return conn
      .preparedQuery(SQL_LOCK)
      .execute(Tuple.of(stateSerialName.hashCode(), lockId))
      .compose { pgRow ->
        if (pgRow.first().getBoolean("locked")) {
          succeededFuture()
        } else {
          failedFuture(LockingException("Can't be locked ${metadata.stateId}"))
        }
      }
  }

  private fun getSnapshot(
    conn: SqlConnection,
    id: UUID
  ): Future<Snapshot<S>?> {
    return conn
      .preparedQuery(GET_EVENTS_BY_ID)
      .execute(Tuple.of(id))
      .map { rowSet ->
        rowSet
          .iterator()
          .asSequence()
          .map { row: Row ->
            val json = JsonObject(row.getValue("event_payload").toString())
            json.put("type", row.getString("event_type"))
            Pair(json.toString(), row.getInteger("version"))
          }.toList()
      }.compose { events ->
        if (events.isEmpty()) {
          succeededFuture(null)
        } else {
          var state: S? = null
          events.forEach {
            val event = json.decodeFromString(eventSerDer, it.first)
            state = config.eventHandler.handleEvent(state, event)
          }
          succeededFuture(Snapshot(state!!, events.last().second))
        }
      }
  }

  private fun appendCommand(conn: SqlConnection, command: C, metadata: CommandMetadata): Future<Void> {
    val cmdAsJson = json.encodeToString(commandSerDer, command)
    log.debug("Will append command {} as {}", command, cmdAsJson)
    val params = Tuple.of(
      metadata.commandId,
      JsonObject(cmdAsJson)
    )
    return conn.preparedQuery(SQL_APPEND_CMD)
      .execute(params)
      .mapEmpty()
  }

  private fun appendEvents(
    conn: SqlConnection,
    initialVersion: Int,
    events: List<E>,
    metadata: CommandMetadata
  ): Future<CommandSideEffect> {
    var version = initialVersion
    val eventIds = events.map { UUID.randomUUID() }
    val tuples: List<Tuple> = events.mapIndexed { index, event ->
      val causationId: UUID = if (index == 0) metadata.commandId else eventIds[(index - 1)]
      val eventAsJson = json.encodeToString(eventSerDer, event)
      val eventId = eventIds[index]
      val jsonObject = JsonObject(eventAsJson)
      val type = jsonObject.getString("type")
      Tuple.of(
        type,
        causationId,
        metadata.correlationId,
        stateSerialName,
        metadata.stateId,
        jsonObject,
        ++version,
        eventId
      )
    }
    val appendedEventList = mutableListOf<Pair<JsonObject, EventMetadata>>()
    return conn.preparedQuery(SQL_APPEND_EVENT)
      .executeBatch(tuples)
      .onSuccess { rowSet ->
        var rs: RowSet<Row>? = rowSet
        tuples.mapIndexed { index, _ ->
          val sequence = rs!!.iterator().next().getLong("sequence")
          val correlationId = tuples[index].getUUID(2)
          val eventId = tuples[index].getUUID(7)
          val eventMetadata = EventMetadata(
            stateSerialName, stateId = metadata.stateId, eventId,
            correlationId, eventId, sequence
          )
          appendedEventList.add(Pair(tuples[index].getJsonObject(5), eventMetadata))
          rs = rs!!.next()
        }
      }.map {
        CommandSideEffect(appendedEventList, version)
      }
  }

  private fun projectEvents(
    conn: SqlConnection,
    commandSideEffect: CommandSideEffect,
    projector: EventsProjector,
    metadata: CommandMetadata,
  ): Future<Void> {
    log.debug("Will project {} events", commandSideEffect.appendedEvents.size)
    val initialFuture = succeededFuture<Void>()
    return commandSideEffect.appendedEvents.fold(
      initialFuture
    ) { currentFuture: Future<Void>, appendedEvent: Pair<JsonObject, EventMetadata> ->
      currentFuture.compose {
        val eventMetadata = EventMetadata(
          stateSerialName,
          metadata.stateId,
          appendedEvent.second.eventId,
          metadata.commandId,
          appendedEvent.second.causationId,
          appendedEvent.second.eventSequence
        )
        projector.project(conn, appendedEvent.first, eventMetadata)
      }
    }.mapEmpty()
  }
}
