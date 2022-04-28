package io.github.crabzilla.command

import io.github.crabzilla.CrabzillaConstants
import io.github.crabzilla.EventMetadata
import io.github.crabzilla.EventProjector
import io.github.crabzilla.EventRecord
import io.github.crabzilla.JsonObjectSerDer
import io.github.crabzilla.core.CommandComponent
import io.github.crabzilla.core.CommandHandler
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.RowStream
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class CommandController<S : Any, C : Any, E : Any>(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val commandComponent: CommandComponent<S, C, E>,
  private val serDer: JsonObjectSerDer<S, C, E>,
  private val options: CommandControllerOptions = CommandControllerOptions()
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    const val GET_EVENTS_BY_ID =
      """
      SELECT event_type, event_payload, version
      FROM events
      WHERE state_id = $1
      ORDER BY sequence
    """
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ INSERT 
            INTO events (event_type, causation_id, correlation_id, state_type, state_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8) returning sequence"""
    const val correlationIdIndex = 2
    const val eventPayloadIndex = 5
    const val currentVersionIndex = 6
    const val eventIdIndex = 7
  }

  private val stateTypeName = commandComponent.stateClassName()
  private val notificationsByStateType = HashSet<String>()
  private val commandHandler: CommandHandler<S, C, E> = commandComponent.commandHandlerFactory.invoke()

  fun startPgNotification(): CommandController<S, C, E> {
    log.info("Starting notifying Postgres for $stateTypeName each ${options.pgNotificationInterval} ms")
    notificationsByStateType.add(stateTypeName)
    vertx.setPeriodic(options.pgNotificationInterval) {
      flushPendingPgNotifications()
    }
    return this
  }

  fun flushPendingPgNotifications(): Future<Void> {
    val initialFuture = Future.succeededFuture<Void>()
    return notificationsByStateType.fold(
      initialFuture
    ) { currentFuture: Future<Void>, stateType: String ->
      currentFuture.compose {
        val query = "NOTIFY ${CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL}, '$stateType'"
        pgPool.preparedQuery(query).execute()
          .onSuccess { log.info("Notified postgres: $query") }
          .mapEmpty()
      }
    }.onFailure {
      log.error("Notification to postgres failed {$stateTypeName}")
    }.onSuccess {
      notificationsByStateType.clear()
    }
  }

  fun handle(metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    return pgPool.withTransaction { conn: SqlConnection ->
      handle(conn, metadata, command)
    }
  }

  fun compose(f: (SqlConnection) -> Future<CommandSideEffect>): Future<CommandSideEffect> {
    return pgPool.withTransaction(f)
  }

  fun handle(conn: SqlConnection, metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    fun validate(command: C): Future<Void> {
      if (commandComponent.commandValidator != null) {
        val errors = commandComponent.commandValidator!!.validate(command)
        if (errors.isNotEmpty()) {
          return Future.failedFuture(CommandException.ValidationException(errors))
        }
      }
      return Future.succeededFuture()
    }
    fun lock(conn: SqlConnection, lockId: Int, metadata: CommandMetadata): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(stateTypeName.hashCode(), lockId))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            Future.succeededFuture()
          } else {
            Future.failedFuture(CommandException.LockingException("Can't be locked ${metadata.stateId}"))
          }
        }
    }
    fun getSnapshot(conn: SqlConnection, id: UUID): Future<Snapshot<S>?> {
      val promise = Promise.promise<Snapshot<S>?>()
      return conn
        .prepare(GET_EVENTS_BY_ID)
        .compose { pq: PreparedStatement ->
          var state: S? = null
          var latestVersion = 0
          var error: Throwable? = null
          // Fetch 1000 rows at a time
          val stream: RowStream<Row> = pq.createStream( options.eventStreamSize, Tuple.of(id))
          // Use the stream
          stream.handler { row: Row ->
            val eventAsJson = JsonObject(row.getValue("event_payload").toString())
            val asEvent = serDer.eventFromJson(eventAsJson)
            latestVersion = row.getInteger("version")
            log.debug("Found event {} version {}", asEvent, latestVersion)
            state = commandComponent.eventHandler.handleEvent(state, asEvent)
            log.debug("State {}", state)
          }
          stream.exceptionHandler { error = it }
          stream.endHandler {
            stream.close()
            log.debug("End of stream")
            if (error != null) {
              promise.fail(error)
            } else {
              if (latestVersion == 0) {
                promise.complete(null)
              } else {
                promise.complete(Snapshot(state!!, latestVersion))
              }
            }
          }
          promise.future()
        }
    }
    fun appendCommand(conn: SqlConnection, command: C, metadata: CommandMetadata): Future<Void> {
      val cmdAsJson = serDer.commandToJson(command)
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(
        metadata.commandId,
        cmdAsJson
      )
      return conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .mapEmpty()
    }
    fun appendEvents(
      conn: SqlConnection,
      initialVersion: Int,
      events: List<E>,
      metadata: CommandMetadata
    ): Future<CommandSideEffect> {
      var resultingVersion = initialVersion
      val eventIds = events.map { UUID.randomUUID() }
      val tuples: List<Tuple> = events.mapIndexed { index, event ->
        val causationId: UUID = if (index == 0) metadata.causationId else eventIds[(index - 1)]
        val eventAsJsonObject = serDer.eventToJson(event)
        val eventId = eventIds[index]
        val type = eventAsJsonObject.getString("type")
        Tuple.of(
          type,
          causationId,
          metadata.correlationId,
          commandComponent.stateClassName(),
          metadata.stateId,
          eventAsJsonObject,
          ++resultingVersion,
          eventId
        )
      }
      val appendedEventList = mutableListOf<EventRecord>()
      return conn.preparedQuery(SQL_APPEND_EVENT)
        .executeBatch(tuples)
        .onSuccess { rowSet ->
          var rs: RowSet<Row>? = rowSet
          tuples.mapIndexed { index, _ ->
            val sequence = rs!!.iterator().next().getLong("sequence")
            val correlationId = tuples[index].getUUID(correlationIdIndex)
            val currentVersion = tuples[index].getInteger(currentVersionIndex)
            val eventId = tuples[index].getUUID(eventIdIndex)
            val eventPayload = tuples[index].getJsonObject(eventPayloadIndex)
            val eventMetadata = EventMetadata(
              stateType = commandComponent.stateClassName(), stateId = metadata.stateId, eventId = eventId,
              correlationId = correlationId, causationId = eventId, eventSequence = sequence, version = currentVersion
            )
            appendedEventList.add(EventRecord(eventMetadata, eventPayload))
            rs = rs!!.next()
          }
        }.map {
          CommandSideEffect(appendedEventList)
        }
    }
    fun projectEvents(conn: SqlConnection, appendedEvents: List<EventRecord>, projector: EventProjector)
            : Future<Void> {
      log.debug("Will project {} events", appendedEvents.size)
      val initialFuture = Future.succeededFuture<Void>()
      return appendedEvents.fold(
        initialFuture
      ) { currentFuture: Future<Void>, appendedEvent: EventRecord ->
        currentFuture.compose {
          projector.project(conn, appendedEvent)
        }
      }.mapEmpty()
    }

    return validate(command)
      .compose {
        log.debug("Command validated")
        lock(conn, metadata.stateId.hashCode(), metadata)
          .compose {
            log.debug("State locked")
            getSnapshot(conn, metadata.stateId)
          }.compose { snapshot ->
            log.debug("Got snapshot {}", snapshot)
            Future.succeededFuture(Pair(snapshot, commandHandler.handleCommand(command, snapshot?.state)))
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
            if (options.eventProjector != null) {
              projectEvents(conn, commandSideEffect.appendedEvents, options.eventProjector!!)
                .onSuccess {
                  log.debug("Events projected")
                }.map { commandSideEffect }
            } else {
              log.debug("EventProjector is null, skipping projecting events")
              Future.succeededFuture(commandSideEffect)
            }
          }.onSuccess {
            if (options.eventBusTopic != null) {
              val message = JsonObject()
                .put("stateType", stateTypeName)
                .put("commandMetadata", metadata.toJsonObject())
                .put("events", it.toJsonArray())
                .put("timestamp", Instant.now())
              vertx.eventBus().publish(options.eventBusTopic, message)
              log.debug("Published to topic ${options.eventBusTopic} the message ${message.encodePrettily()}")
            }
            notificationsByStateType.add(stateTypeName)
          }
      }
  }


}
