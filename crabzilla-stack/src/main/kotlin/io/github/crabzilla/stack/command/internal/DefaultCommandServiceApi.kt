package io.github.crabzilla.stack.command.internal

import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.stack.*
import io.github.crabzilla.stack.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceConfig
import io.github.crabzilla.stack.command.CommandServiceException
import io.github.crabzilla.stack.command.CommandServiceException.ConcurrencyException
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.*
import org.slf4j.LoggerFactory
import java.time.Instant

internal class DefaultCommandServiceApi<S : Any, C : Any, E : Any>(
  private val crabzillaContext: CrabzillaContext,
  private val commandServiceConfig: CommandServiceConfig<S, C, E>,
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
      """ INSERT INTO commands (state_id, causation_id, last_causation_id, cmd_payload)
          VALUES ($1, $2, $3, $4)"""
    const val correlationIdIndex = 2
    const val eventPayloadIndex = 5
    const val currentVersionIndex = 6
    const val eventIdIndex = 7
  }

  private val streamName = commandServiceConfig.stateClass.simpleName!!
  private val log = LoggerFactory.getLogger("${DefaultCommandServiceApi::class.java.simpleName}-$streamName")

  override fun handle(stateId: String, command: C, versionPredicate: ((Int) -> Boolean)?): Future<EventMetadata> {
    return crabzillaContext.pgPool().withTransaction { conn: SqlConnection ->
      handle(conn, stateId, command, versionPredicate)
    }
  }

  override fun withinTransaction(f: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata> {
    return crabzillaContext.pgPool().withTransaction(f)
  }

  override fun handle(conn: SqlConnection, stateId: String, command: C, versionPredicate: ((Int) -> Boolean)?)
    : Future<EventMetadata> {

    fun lock(conn: SqlConnection): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(streamName.hashCode(), stateId.hashCode()))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            succeededFuture()
          } else {
            failedFuture(ConcurrencyException("Can't be locked $stateId"))
          }
        }
    }

    fun appendEvents(
      conn: SqlConnection,
      snapshot: Snapshot<S>,
      events: List<E>,
    ): Future<List<EventRecord>> {
      log.debug("Will append events {}", events)
      var resultingVersion = snapshot.version
      val eventIds = events.map { crabzillaContext.nextUlid() }
      val causationIds = eventIds.toMutableList()
      val correlationIds = eventIds.toMutableList()
      val tuples = events.mapIndexed { index, event ->
        correlationIds[index] = snapshot.correlationId ?: causationIds[0]
        val eventAsJsonObject = serDer.eventToJson(event)
        val eventId = eventIds[index]
        val type = eventAsJsonObject.getString("type")
        if (index == 0) {
          causationIds[0] = snapshot.causationId ?: eventIds[0]
        } else {
          causationIds[index] = eventIds[(index - 1)]
        }
        Tuple.of(type, causationIds[index], correlationIds[index], streamName,
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
            val correlationId = tuples[index].getString(correlationIdIndex)
            val currentVersion = tuples[index].getInteger(currentVersionIndex)
            val eventId = tuples[index].getString(eventIdIndex)
            val eventPayload = tuples[index].getJsonObject(eventPayloadIndex)
            val eventMetadata = EventMetadata(
              stateType = streamName, stateId = stateId, eventId = eventId,
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

    fun appendCommand(conn: SqlConnection, cmdAsJson: JsonObject, stateId: String,
                      causationId: String, lastCausationId: String): Future<Void> {
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(stateId, causationId, lastCausationId, cmdAsJson)
      return conn.preparedQuery(SQL_APPEND_CMD).execute(params).mapEmpty()
    }

    return lock(conn)
      .compose {
        log.debug("State locked {}", stateId.hashCode())
        getSnapshot(conn, stateId)
      }.compose { snapshot: Snapshot<S> ->
        log.debug("Got snapshot {}", snapshot)
        if (versionPredicate != null && !versionPredicate.invoke(snapshot.version)) {
          val error = "Current version ${snapshot.version} is invalid"
          failedFuture(ConcurrencyException(error))
        } else {
          try {
            val session = commandServiceConfig.commandHandler.handle(command, snapshot.state)
            succeededFuture(Pair(snapshot, session))
          } catch (e: Exception) {
            val error = CommandServiceException.BusinessException(e.message ?: "Unknown")
            failedFuture(error)
          }
        }
      }.compose { pair ->
        val (snapshot: Snapshot<S>, session: CommandSession<S, E>) = pair
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
        val (_, _, appendedEvents) = it
        val cmdAsJson = serDer.commandToJson(command)
        if (options.persistCommands) {
          appendCommand(conn, cmdAsJson, stateId,
            appendedEvents.first().metadata.causationId,
            appendedEvents.last().metadata.causationId)
            .map { Pair(appendedEvents, cmdAsJson) }
        } else {
          succeededFuture(Pair(appendedEvents, cmdAsJson))
        }
      }.compose {
        log.debug("Command was appended")
        val (appendedEvents, cmdAsJson) = it
        if (options.eventBusTopic != null) {
          val message = JsonObject()
            .put("stateType", streamName)
            .put("stateId", stateId.toString())
            .put("command", cmdAsJson)
            .put("events", JsonArray(appendedEvents.map { e -> e.toJsonObject() }))
            .put("timestamp", Instant.now())
          crabzillaContext.vertx().eventBus().publish(options.eventBusTopic, message)
          log.debug("Published to topic ${options.eventBusTopic} the message ${message.encodePrettily()}")
        }
        succeededFuture(appendedEvents.last().metadata)
      }.onSuccess {
        val query = "NOTIFY $POSTGRES_NOTIFICATION_CHANNEL, '$streamName'"
        crabzillaContext.pgPool().preparedQuery(query).execute()
          .onSuccess { log.debug("Notified postgres: $query") }
        log.debug("Transaction committed")
      }.onFailure {
        log.debug("Transaction aborted {}", it.message)
      }
  }

  private fun getSnapshot(conn: SqlConnection, id: String): Future<Snapshot<S>> {
    val promise = Promise.promise<Snapshot<S>>()
    return conn
      .prepare(GET_EVENTS_BY_ID)
      .compose { pq: PreparedStatement ->
        var state: S = commandServiceConfig.initialState
        var latestVersion = 0
        var lastCausationId: String? = null
        var lastCorrelationId: String? = null
        var error: Throwable? = null
        // Fetch 1000 rows at a time
        val stream: RowStream<Row> = pq.createStream(options.eventStreamSize, Tuple.of(id))
        // Use the stream
        stream.handler { row: Row ->
          latestVersion = row.getInteger("version")
          lastCausationId = row.getString("id")
          lastCorrelationId = row.getString("correlation_id")
          log.debug("Found event version {}, causationId {}, correlationId {}",
            latestVersion, lastCausationId, lastCorrelationId)
          state = commandServiceConfig.eventHandler
            .handle(state, serDer.eventFromJson(JsonObject(row.getValue("event_payload").toString())))
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
              promise.complete(Snapshot(state, latestVersion, null, null))
            } else {
              promise.complete(Snapshot(state, latestVersion, lastCausationId!!, lastCorrelationId!!))
            }
          }
        }
        promise.future()
      }
  }

}
