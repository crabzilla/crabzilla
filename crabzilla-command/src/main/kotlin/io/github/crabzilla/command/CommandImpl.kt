package io.github.crabzilla.command

import CrabzillaContext
import CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import EventMetadata
import EventProjector
import EventRecord
import io.github.crabzilla.core.CommandsSession
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.RowStream
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.time.Instant

class DefaultCommandComponent<S : Any, C : Any, E : Any>(
  private val crabzillaContext: CrabzillaContext,
  private val config: CommandComponentConfig<S, C, E>,
) : CommandComponent<C> {
  private val stateType = config.stateClass.simpleName!!
  private val log = LoggerFactory.getLogger("${DefaultCommandComponent::class.java.simpleName}-$stateType")

  override fun handle(
    stateId: String,
    command: C,
    versionPredicate: ((Int) -> Boolean)?,
  ): Future<EventMetadata> {
    return crabzillaContext.pgPool().withTransaction { conn: SqlConnection ->
      handle(conn, stateId, command, versionPredicate)
    }
  }

  override fun withinTransaction(f: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata> {
    return crabzillaContext.pgPool().withTransaction(f)
  }

  override fun handle(
    conn: SqlConnection,
    stateId: String,
    command: C,
    versionPredicate: ((Int) -> Boolean)?,
  ): Future<EventMetadata> {
    fun lock(conn: SqlConnection): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(stateType.hashCode(), stateId.hashCode()))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            succeededFuture()
          } else {
            failedFuture(CommandComponentException.ConcurrencyException("Can't be locked $stateId"))
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
      val tuples: List<Tuple> =
        events.mapIndexed { index, event ->
          correlationIds[index] = snapshot.correlationId ?: causationIds[0]
          val eventAsJsonObject = config.eventSerDer.toJson(event)
          val eventId = eventIds[index]
          val type = eventAsJsonObject.getString("type")
          if (index == 0) {
            causationIds[0] = snapshot.causationId ?: eventIds[0]
          } else {
            causationIds[index] = eventIds[(index - 1)]
          }
          Tuple.of(
            type,
            causationIds[index],
            correlationIds[index],
            stateType,
            stateId,
            eventAsJsonObject,
            ++resultingVersion,
            eventId,
          )
        }
      val appendedEventList = mutableListOf<EventRecord>()
      return conn.preparedQuery(SQL_APPEND_EVENT)
        .executeBatch(tuples)
        .onSuccess { rowSet ->
          var rs: RowSet<Row>? = rowSet
          List(tuples.size) { index ->
            val sequence = rs!!.iterator().next().getLong("sequence")
            val correlationId = tuples[index].getString(CORRELATION_ID_INDEX)
            val currentVersion = tuples[index].getInteger(CURRENT_VERSION_INDEX)
            val eventId = tuples[index].getString(EVENT_ID_INDEX)
            val eventPayload = tuples[index].getJsonObject(EVENT_PAYLOAD_INDEX)
            val eventMetadata =
              EventMetadata(
                stateType = stateType,
                stateId = stateId,
                eventId = eventId,
                correlationId = correlationId,
                causationId = eventId,
                eventSequence = sequence,
                version = currentVersion,
                tuples[index].getString(0),
              )
            appendedEventList.add(EventRecord(eventMetadata, eventPayload))
            rs = rs!!.next()
          }
        }.map {
          appendedEventList
        }
    }

    fun projectEvents(
      conn: SqlConnection,
      appendedEvents: List<EventRecord>,
      subscription: EventProjector,
    ): Future<Void> {
      log.debug("Will project {} events", appendedEvents.size)
      val initialFuture = succeededFuture<Void>()
      return appendedEvents.fold(
        initialFuture,
      ) { currentFuture: Future<Void>, appendedEvent: EventRecord ->
        currentFuture.compose {
          subscription.project(conn, appendedEvent)
        }
      }.mapEmpty()
    }

    fun appendCommand(
      conn: SqlConnection,
      cmdAsJson: JsonObject,
      stateId: String,
      causationId: String,
      lastCausationId: String,
    ): Future<Void> {
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
          failedFuture(CommandComponentException.ConcurrencyException(error))
        } else {
          try {
            log.debug("Will handle command {} on state {}", command, snapshot)
            val session =
              CommandsSession(
                snapshot.state,
                commandHandler = config.commandHandler,
                eventHandler = config.eventHandler,
              )
            session.handle(command)
            succeededFuture(Pair(snapshot, session))
          } catch (e: Exception) {
            val error = CommandComponentException.BusinessException(e.message ?: "Unknown", e)
            failedFuture(error)
          }
        }
      }.compose { pair ->
        val (snapshot: Snapshot<S>, session: CommandsSession<C, S, E>) = pair
        log.debug("Command handled")
        appendEvents(conn, snapshot, session.appliedEvents())
          .map { Triple(snapshot, session, it) }
      }.compose { triple ->
        val (_, _, appendedEvents) = triple
        log.debug("Events appended {}", appendedEvents)
        if (config.eventProjector != null) {
          projectEvents(conn, appendedEvents, config.eventProjector)
            .onSuccess {
              log.debug("Events projected")
            }.map { triple }
        } else {
          log.debug("EventProjector is null, skipping projecting events")
          succeededFuture(triple)
        }
      }.compose {
        val (_, _, appendedEvents) = it
        if (config.commandSerDer == null) {
          succeededFuture(Pair(appendedEvents, null))
        } else {
          val cmdAsJson = config.commandSerDer.toJson(command)
          appendCommand(
            conn,
            cmdAsJson,
            stateId,
            appendedEvents.first().metadata.causationId,
            appendedEvents.last().metadata.causationId,
          )
            .map { Pair(appendedEvents, cmdAsJson) }
        }
      }.compose {
        log.debug("Command was appended")
        val (appendedEvents, cmdAsJson) = it
        if (config.eventBusTopic != null) { // TODO also track command failures
          val message =
            JsonObject()
              .put("stateType", stateType)
              .put("stateId", stateId)
              .put("events", JsonArray(appendedEvents.map { e -> e.toJsonObject() }))
              .put("timestamp", Instant.now())
          if (cmdAsJson != null) {
            message.put("command", cmdAsJson)
          }
          crabzillaContext.vertx().eventBus().publish(config.eventBusTopic, message)
          log.debug("Published to topic ${config.eventBusTopic} the message ${message.encodePrettily()}")
        }
        succeededFuture(appendedEvents.last().metadata)
      }.onSuccess {
        val query = "NOTIFY $POSTGRES_NOTIFICATION_CHANNEL, '$stateType'"
        conn.preparedQuery(query).execute()
          .onSuccess { log.debug("Notified postgres: $query") }
        log.debug("Transaction committed")
      }.onFailure {
        log.debug("Transaction aborted {}", it.message)
      }
  }

  private fun getSnapshot(
    conn: SqlConnection,
    id: String,
  ): Future<Snapshot<S>> {
    val promise = Promise.promise<Snapshot<S>>()
    return conn
      .prepare(GET_EVENTS_BY_ID)
      .compose { pq: PreparedStatement ->
        var state: S = config.initialState
        var latestVersion = 0
        var lastCausationId: String? = null
        var lastCorrelationId: String? = null
        var error: Throwable? = null
        // Fetch eventStreamSize rows at a time
        val stream: RowStream<Row> = pq.createStream(config.eventStreamSize, Tuple.of(id))
        // Use the stream
        stream.handler { row: Row ->
          latestVersion = row.getInteger("version")
          lastCausationId = row.getString("id")
          lastCorrelationId = row.getString("correlation_id")
          log.debug(
            "Found event version {}, causationId {}, correlationId {}",
            latestVersion,
            lastCausationId,
            lastCorrelationId,
          )
          state =
            config.eventHandler
              .invoke(state, config.eventSerDer.fromJson(row.getJsonObject("event_payload")))
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
    const val CORRELATION_ID_INDEX = 2
    const val EVENT_PAYLOAD_INDEX = 5
    const val CURRENT_VERSION_INDEX = 6
    const val EVENT_ID_INDEX = 7
  }
}

internal data class Snapshot<S : Any>(
  val state: S,
  val version: Int,
  val causationId: String?,
  val correlationId: String?,
)
