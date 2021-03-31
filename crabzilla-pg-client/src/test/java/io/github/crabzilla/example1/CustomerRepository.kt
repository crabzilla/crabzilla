package io.github.crabzilla.example1

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple

/**
 * Read model repository
 */
class CustomerRepository(private val pool: PgPool) {
  fun upsert(id: Int, name: String, isActive: Boolean): Future<Void> {
    val promise = Promise.promise<Void>()
    pool
      .preparedQuery(
        "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) ON CONFLICT (id) DO UPDATE " +
          "SET name = $2, is_active = $3"
      )
      .execute(Tuple.of(id, name, isActive)) { ar ->
        if (ar.succeeded()) {
          promise.complete()
        } else {
          promise.fail(ar.cause().message)
        }
      }
    return promise.future()
  }

  fun updateStatus(id: Int, isActive: Boolean): Future<Void> {
    val promise = Promise.promise<Void>()
    pool
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id)) { ar ->
        if (ar.succeeded()) {
          promise.complete()
        } else {
          promise.fail(ar.cause().message)
        }
      }
    return promise.future()
  }
}
