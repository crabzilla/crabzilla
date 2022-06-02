package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.*

interface CommandServiceApi<C : Any> {

  fun getCurrentVersion(stateId: UUID): Future<Int>

  fun handle(stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)? = null): Future<List<EventRecord>>

  fun withinTransaction(f: (SqlConnection) -> Future<List<EventRecord>>): Future<List<EventRecord>>

  fun handle(conn: SqlConnection, stateId: UUID, command: C, versionPredicate: ((Int) -> Boolean)? = null)
    : Future<List<EventRecord>>

}
