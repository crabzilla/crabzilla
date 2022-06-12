package io.github.crabzilla.todo//package io.github.crabzilla.stack.store
//
//import io.github.crabzilla.stack.EventMetadata
//import io.github.crabzilla.stack.EventRecord
//import io.github.crabzilla.stack.command.internal.DefaultCommandServiceApi
//import io.vertx.core.Future
//import io.vertx.core.json.JsonObject
//import io.vertx.sqlclient.Row
//import io.vertx.sqlclient.RowSet
//import io.vertx.sqlclient.SqlConnection
//import io.vertx.sqlclient.Tuple
//import java.util.*
//
//class EventStoreImpl : EventStore {
//
//
//  fun appendEvents(conn: SqlConnection,
//                   events: List<JsonObject>,
//                   originalMetadata: EventMetadata?)
//  : Future<List<EventRecord>> {
//    var resultingVersion = originalMetadata?.version ?: 0
//    val eventIds = events.map { UUID.randomUUID() }
//    val causationIds = eventIds.toMutableList()
//    val correlationIds = eventIds.toMutableList()
//    val tuples = events.mapIndexed { index, eventAsJsonObject ->
//      correlationIds[index] = originalMetadata?.correlationId ?: causationIds[0]
//      val eventId = eventIds[index]
//      val type = eventAsJsonObject.getString("type")
//      if (index == 0) {
//        causationIds[0] = originalMetadata?.causationId ?: eventIds[0]
//      } else {
//        causationIds[index] = eventIds[(index - 1)]
//      }
//      Tuple.of(type, causationIds[index], correlationIds[index], featureComponent.streamName(),
//        stateId, eventAsJsonObject, ++resultingVersion, eventId
//      )
//    }
//    val appendedEventList = mutableListOf<EventRecord>()
//    return conn.preparedQuery(DefaultCommandServiceApi.SQL_APPEND_EVENT)
//      .executeBatch(tuples)
//      .onSuccess { rowSet ->
//        var rs: RowSet<Row>? = rowSet
//        List(tuples.size) { index ->
//          val sequence = rs!!.iterator().next().getLong("sequence")
//          val correlationId = tuples[index].getUUID(DefaultCommandServiceApi.correlationIdIndex)
//          val currentVersion = tuples[index].getInteger(DefaultCommandServiceApi.currentVersionIndex)
//          val eventId = tuples[index].getUUID(DefaultCommandServiceApi.eventIdIndex)
//          val eventPayload = tuples[index].getJsonObject(DefaultCommandServiceApi.eventPayloadIndex)
//          val eventMetadata = EventMetadata(
//            stateType = featureComponent.streamName(), stateId = stateId, eventId = eventId,
//            correlationId = correlationId, causationId = eventId, eventSequence = sequence, version = currentVersion,
//            tuples[index].getString(0)
//          )
//          appendedEventList.add(EventRecord(eventMetadata, eventPayload))
//          rs = rs!!.next()
//        }
//      }.map {
//        appendedEventList
//      }
//  }
//}
