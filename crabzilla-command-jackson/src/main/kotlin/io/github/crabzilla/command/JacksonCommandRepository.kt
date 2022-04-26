package io.github.crabzilla.command

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.CommandSideEffect
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.PgCommandRepository
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
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.reflect.KClass

class JacksonCommandRepository<S : Any, C : Any, E : Any>(private val json: ObjectMapper)
  : PgCommandRepository<S, C, E>() {

  companion object {
    private val log = LoggerFactory.getLogger(JacksonCommandRepository::class.java)
  }

  override fun getSnapshot(
    conn: SqlConnection,
    id: UUID,
    eventClass: KClass<E>,
    eventHandler: EventHandler<S, E>,
    eventStreamSize: Int
  ): Future<Snapshot<S>?> {
    val promise = Promise.promise<Snapshot<S>?>()
    return conn
      .prepare(GET_EVENTS_BY_ID)
      .compose { pq: PreparedStatement ->
        var state: S? = null
        var latestVersion = 0
        var error: Throwable? = null
        // Fetch N rows at a time
        val stream: RowStream<Row> = pq.createStream(eventStreamSize, Tuple.of(id))
        // Use the stream
        stream.handler { row: Row ->
          val eventAsJson = JsonObject(row.getValue("event_payload").toString())
          eventAsJson.put("type", row.getString("event_type"))
          val asEvent = json.readValue(eventAsJson.toString(), eventClass.java)
          latestVersion = row.getInteger("version")
          log.debug("Found event {} version {}", asEvent, latestVersion)
          state = eventHandler.handleEvent(state, asEvent)
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
    val cmdAsJson = json.writeValueAsString(command)
    log.debug("Will append command {} as {}", command, cmdAsJson)
    val params = Tuple.of(
      metadata.commandId,
      JsonObject(cmdAsJson)
    )
    return conn.preparedQuery(SQL_APPEND_CMD)
      .execute(params)
      .mapEmpty()
  }

  override fun appendEvents(conn: SqlConnection,
                            initialVersion: Int,
                            events: List<E>,
                            metadata: CommandMetadata,
                            stateTypeName: String)
  : Future<CommandSideEffect> {
    var resultingVersion = initialVersion
    val eventIds = events.map { UUID.randomUUID() }
    val tuples: List<Tuple> = events.mapIndexed { index, event ->
      val causationId: UUID = if (index == 0) metadata.causationId else eventIds[(index - 1)]
      val eventAsJson = json.writeValueAsString(event)
      val eventId = eventIds[index]
      val jsonObject = JsonObject(eventAsJson)
      val type = jsonObject.getString("type")
      Tuple.of(
        type,
        causationId,
        metadata.correlationId,
        stateTypeName,
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
            stateType = stateTypeName, stateId = metadata.stateId, eventId = eventId,
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
