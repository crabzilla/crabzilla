package io.github.crabzilla.example1

import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import java.util.UUID

/**
 * Read model repository
 */
class CustomerRepository(private val pool: PgPool) {
  fun upsert(id: UUID, name: String, isActive: Boolean): Future<Void> {
    return pool
      .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)")
      .execute(Tuple.of(id, name, isActive))
      .mapEmpty()
  }

  fun updateStatus(id: UUID, isActive: Boolean): Future<Void> {
    return pool
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
