package io.github.crabzilla.command

import io.github.crabzilla.core.CommandComponent
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.CommandRepository
import io.github.crabzilla.stack.CommandSideEffect
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.Snapshot
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.RowStream
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class KotlinxCommandRepository<S: Any, C: Any, E: Any>(
  private val json: Json,
  private val commandComponent: CommandComponent<S, C, E>) : CommandRepository<S, C, E>(commandComponent) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinxCommandRepository::class.java)
  }

  private val eventSerDer = PolymorphicSerializer(commandComponent.eventClass)
  private val commandSerDer = PolymorphicSerializer(commandComponent.commandClass)

  override fun getSnapshot(conn: SqlConnection, id: UUID, eventStreamSize: Int): Future<Snapshot<S>?> {
    val promise = Promise.promise<Snapshot<S>?>()
    return conn
      .prepare(GET_EVENTS_BY_ID)
      .compose { pq: PreparedStatement ->
        var state: S? = null
        var latestVersion = 0
        var error: Throwable? = null
        // Fetch 1000 rows at a time
        val stream: RowStream<Row> = pq.createStream(eventStreamSize, Tuple.of(id))
        // Use the stream
        stream.handler { row: Row ->
          val eventAsJson = JsonObject(row.getValue("event_payload").toString())
          eventAsJson.put("type", row.getString("event_type"))
          val asEvent = json.decodeFromString(eventSerDer, eventAsJson.toString())
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

  override fun appendCommand(conn: SqlConnection, command: C, metadata: CommandMetadata): Future<Void> {
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

  override fun appendEvents(
    conn: SqlConnection,
    initialVersion: Int,
    events: List<E>,
    metadata: CommandMetadata
  )
  : Future<CommandSideEffect> {
    var resultingVersion = initialVersion
    val eventIds = events.map { UUID.randomUUID() }
    val tuples: List<Tuple> = events.mapIndexed { index, event ->
      val causationId: UUID = if (index == 0) metadata.causationId else eventIds[(index - 1)]
      val eventAsJson = json.encodeToString(eventSerDer, event)
      val eventId = eventIds[index]
      val jsonObject = JsonObject(eventAsJson)
      val type = jsonObject.getString("type")
      Tuple.of(
        type,
        causationId,
        metadata.correlationId,
        commandComponent.stateClassName(),
        metadata.stateId,
        jsonObject,
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

}
