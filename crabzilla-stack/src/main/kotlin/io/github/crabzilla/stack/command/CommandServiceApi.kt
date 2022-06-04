package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.*

interface CommandServiceApi<C : Any> {

  fun handle(stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)? = null): Future<EventMetadata>

  fun withinTransaction(f: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata>

  fun handle(conn: SqlConnection, stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)? = null)
    : Future<EventMetadata>

}
