package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

interface CommandServiceApi<C : Any> {

  fun handle(stateId: String, command: C, versionPredicate: ((Int) -> Boolean)? = null): Future<EventMetadata>

  fun withinTransaction(f: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata>

  fun handle(conn: SqlConnection, stateId: String, command: C, versionPredicate: ((Int) -> Boolean)? = null)
    : Future<EventMetadata>

}
