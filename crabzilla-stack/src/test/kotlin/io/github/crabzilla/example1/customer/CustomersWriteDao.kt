package io.github.crabzilla.example1.customer

import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

object CustomersWriteDao {

  fun upsert(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO customer_summary (id, is_active) VALUES ($1, $2)")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }

  fun updateName(conn: SqlConnection, id: UUID, name: String): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set name = $2 WHERE id = $1")
      .execute(Tuple.of(id, name))
      .mapEmpty()
  }
  fun updateStatus(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
