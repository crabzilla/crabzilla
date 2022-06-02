package io.github.crabzilla.stack.command.internal

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.core.FeatureSession
import io.github.crabzilla.stack.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.JsonObjectSerDer
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceException
import io.github.crabzilla.stack.command.CommandServiceException.ConcurrencyException
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class CommandService<S : Any, C : Any, E : Any>(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val featureComponent: FeatureComponent<S, C, E>,
  private val serDer: JsonObjectSerDer<S, C, E>,
  private val options: CommandServiceOptions = CommandServiceOptions(),
) : CommandServiceApi<C> {

  companion object {
    const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    const val GET_EVENTS_BY_ID =
      """
      SELECT id, event_type, event_payload, version, causation_id, correlation_id
        FROM events
       WHERE state_id = $1
       ORDER BY sequence
    """
    const val SQL_APPEND_EVENT =
      """ INSERT
            INTO events (event_type, causation_id, correlation_id, state_type, state_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8) returning sequence"""
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (state_id, causation_id, cmd_payload)
          VALUES ($1, $2, $3)"""
    const val correlationIdIndex = 2
    const val eventPayloadIndex = 5
    const val currentVersionIndex = 6
    const val eventIdIndex = 7
  }

  private val stateTypeName = featureComponent.stateClassName()
  private val log = LoggerFactory.getLogger("${CommandService::class.java.simpleName}-$stateTypeName")
  private val commandHandler: CommandHandler<S, C, E> = featureComponent.commandHandlerFactory.invoke()

  override fun getCurrentVersion(stateId: UUID): Future<Int> {
    return pgPool.withConnection { conn ->
      getSnapshot(conn, stateId)
        .map { it?.version ?: 0 }
    }
  }

  override fun handle(stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)?): Future<EventMetadata> {
    return pgPool.withTransaction { conn: SqlConnection ->
      handle(conn, stateId, command, versionPredicate)
    }
  }

  override fun withinTransaction(f: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata> {
    return pgPool.withTransaction(f)
  }

  override fun handle(conn: SqlConnection, stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)?)
  : Future<EventMetadata> {

    fun validate(command: C): Future<Void> {
      if (featureComponent.commandValidator != null) {
        val errors = featureComponent.commandValidator!!.validate(command)
        if (errors.isNotEmpty()) {
          return failedFuture(CommandServiceException.ValidationException(errors))
        }
      }
      return succeededFuture()
    }

    fun lock(conn: SqlConnection): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(stateTypeName.hashCode(), stateId.hashCode()))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            succeededFuture()
          } else {
            failedFuture(ConcurrencyException("Can't be locked ${stateId}"))
          }
        }
    }

    fun appendEvents(
      conn: SqlConnection,
      snapshot: Snapshot<S>?,
      events: List<E>,
    ): Future<List<EventRecord>> {
      var resultingVersion = snapshot?.version ?: 0
      val eventIds = events.map { UUID.randomUUID() }
      val causationIds = eventIds.toMutableList()
      val correlationIds = eventIds.toMutableList()
      val tuples = events.mapIndexed { index, event ->
        correlationIds[index] = snapshot?.correlationId ?: causationIds[0]
        val eventAsJsonObject = serDer.eventToJson(event)
        val eventId = eventIds[index]
        val type = eventAsJsonObject.getString("type")
        if (index == 0) {
          causationIds[0] = snapshot?.causationId ?: eventIds[0]
        } else {
          causationIds[index] = eventIds[(index - 1)]
        }
        Tuple.of(type, causationIds[index], correlationIds[index], featureComponent.stateClassName(),
          stateId, eventAsJsonObject, ++resultingVersion, eventId
        )
      }
      val appendedEventList = mutableListOf<EventRecord>()
      return conn.preparedQuery(SQL_APPEND_EVENT)
        .executeBatch(tuples)
        .onSuccess { rowSet ->
          var rs: RowSet<Row>? = rowSet
          List(tuples.size) { index ->
            val sequence = rs!!.iterator().next().getLong("sequence")
            val correlationId = tuples[index].getUUID(correlationIdIndex)
            val currentVersion = tuples[index].getInteger(currentVersionIndex)
            val eventId = tuples[index].getUUID(eventIdIndex)
            val eventPayload = tuples[index].getJsonObject(eventPayloadIndex)
            val eventMetadata = EventMetadata(
              stateType = featureComponent.stateClassName(), stateId = stateId, eventId = eventId,
              correlationId = correlationId, causationId = eventId, eventSequence = sequence, version = currentVersion,
              tuples[index].getString(0)
            )
            appendedEventList.add(EventRecord(eventMetadata, eventPayload))
            rs = rs!!.next()
          }
        }.map {
          appendedEventList
        }
    }

    fun projectEvents(conn: SqlConnection, appendedEvents: List<EventRecord>, subscription: EventProjector)
            : Future<Void> {
      log.debug("Will project {} events", appendedEvents.size)
      val initialFuture = succeededFuture<Void>()
      return appendedEvents.fold(
        initialFuture
      ) { currentFuture: Future<Void>, appendedEvent: EventRecord ->
        currentFuture.compose {
          subscription.project(conn, appendedEvent)
        }
      }.mapEmpty()
    }

    fun appendCommand(conn: SqlConnection, cmdAsJson: JsonObject, stateId: UUID, causationId: UUID): Future<Void> {
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(stateId, causationId, cmdAsJson)
      return conn.preparedQuery(SQL_APPEND_CMD).execute(params).mapEmpty()
    }

    return validate(command)
      .compose {
        log.debug("Command validated")
        lock(conn)
          .compose {
            log.debug("State locked {}", stateId.hashCode())
            getSnapshot(conn, stateId)
          }.compose { snapshot: Snapshot<S>? ->
            if (snapshot == null) {
              succeededFuture(null)
            } else {
              succeededFuture(snapshot)
            }
          }.compose { snapshot: Snapshot<S>? ->
            log.debug("Got snapshot {}", snapshot)
            if (versionPredicate != null && !versionPredicate.invoke(snapshot?.version?:0)) {
              val error = "Current version ${snapshot?.version?:0} is invalid"
              failedFuture(ConcurrencyException(error))
            } else {
              try {
                val session = commandHandler.handleCommand(command, snapshot?.state)
                succeededFuture(Pair(snapshot, session))
              } catch (e: Exception) {
                val error = CommandServiceException.BusinessException(e.message ?: "Unknown")
                failedFuture(error)
              }
            }
          }.compose { pair ->
            val (snapshot: Snapshot<S>?, session: FeatureSession<S, E>) = pair
            log.debug("Command handled")
            appendEvents(conn, snapshot, session.appliedEvents())
              .map { Triple(snapshot, session, it) }
          }.compose { triple ->
            val (_, _, appendedEvents) = triple
            log.debug("Events appended {}", appendedEvents)
            if (options.eventProjector != null) {
              projectEvents(conn, appendedEvents, options.eventProjector)
                .onSuccess {
                  log.debug("Events projected")
                }.map { triple }
            } else {
              log.debug("EventProjector is null, skipping projecting events")
              succeededFuture(triple)
            }
          }.compose {
            val (snapshot, _, appendedEvents) = it
            val cmdAsJson = serDer.commandToJson(command)
            appendCommand(conn, cmdAsJson, stateId,
              snapshot?.causationId ?: appendedEvents.first().metadata.causationId)
              .map { Pair(appendedEvents, cmdAsJson) }
          }.compose {
            log.debug("Command was appended")
            val (appendedEvents, cmdAsJson) = it
            if (options.eventBusTopic != null) {
              val message = JsonObject()
                .put("stateType", stateTypeName)
                .put("stateId", stateId.toString())
                .put("command", cmdAsJson)
                .put("events", JsonArray(appendedEvents.map { it.toJsonObject() }))
                .put("timestamp", Instant.now())
              vertx.eventBus().publish(options.eventBusTopic, message)
              log.debug("Published to topic ${options.eventBusTopic} the message ${message.encodePrettily()}")
            }
            succeededFuture(appendedEvents.last().metadata)
          }
      }.onSuccess {
        val query = "NOTIFY $POSTGRES_NOTIFICATION_CHANNEL, '$stateTypeName'"
        pgPool.preparedQuery(query).execute()
          .onSuccess { log.debug("Notified postgres: $query") }
        log.debug("Transaction committed")
      }.onFailure {
        log.debug("Transaction aborted {}", it.message)
      }
  }

  private fun getSnapshot(conn: SqlConnection, id: UUID): Future<Snapshot<S>?> {
    val promise = Promise.promise<Snapshot<S>?>()
    return conn
      .prepare(GET_EVENTS_BY_ID)
      .compose { pq: PreparedStatement ->
        var state: S? = null
        var latestVersion = 0
        var lastCausationId: UUID? = null
        var lastCorrelationId: UUID? = null
        var error: Throwable? = null
        // Fetch 1000 rows at a time
        val stream: RowStream<Row> = pq.createStream(options.eventStreamSize, Tuple.of(id))
        // Use the stream
        stream.handler { row: Row ->
          val eventAsJson = JsonObject(row.getValue("event_payload").toString())
          val asEvent = serDer.eventFromJson(eventAsJson)
          latestVersion = row.getInteger("version")
          lastCausationId = row.getUUID("id")
          lastCorrelationId = row.getUUID("correlation_id")
          log.debug("Found event {} version {}, causationId {}, correlationId {}", asEvent,
            latestVersion, lastCausationId, lastCorrelationId)
          state = featureComponent.eventHandler.handleEvent(state, asEvent)
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
              promise.complete(Snapshot(state!!, latestVersion, lastCausationId!!, lastCorrelationId!!))
            }
          }
        }
        promise.future()
      }
  }

}
