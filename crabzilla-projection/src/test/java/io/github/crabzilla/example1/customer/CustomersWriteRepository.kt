package io.github.crabzilla.example1.customer

import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.UUID

/**
 * Read model repository
 */
class CustomersWriteRepository(private val viewName: String) {

  fun upsert(conn: SqlConnection, id: UUID, name: String, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO $viewName (id, name, is_active) VALUES ($1, $2, $3)")
      .execute(Tuple.of(id, name, isActive))
      .mapEmpty()
  }

  fun updateStatus(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("UPDATE $viewName set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
