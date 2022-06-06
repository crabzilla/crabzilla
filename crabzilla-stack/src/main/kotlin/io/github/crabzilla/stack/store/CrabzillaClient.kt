package io.github.crabzilla.stack.store

import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

interface CrabzillaClient {

  // get snapshot
  // lock state id
  // append events
  // append command
  // project events

  fun append(conn: SqlConnection,
             events: List<JsonObject>,
             originalMetadata: EventMetadata? = null)
    : Future<List<EventRecord>>

  fun append(conn: SqlConnection,
             events: List<JsonObject>,
             versionPredicate: ((Int) -> Boolean)? = null)
  : Future<List<EventRecord>>

}
