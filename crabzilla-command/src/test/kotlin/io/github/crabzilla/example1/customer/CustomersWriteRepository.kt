package io.github.crabzilla.example1.customer

import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.UUID

/**
 * Read model repository
 */
object CustomersWriteRepository {

  fun upsert(conn: SqlConnection, id: UUID, name: String, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)")
      .execute(Tuple.of(id, name, isActive))
      .mapEmpty()
  }

  fun updateStatus(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
