package io.github.crabzilla.example1

import io.github.crabzilla.core.EventPublisher
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple

/**
 * To update customer read model given events
 */
class CustomerReadModelProjector(private val repo: CustomerRepository) : EventPublisher<CustomerEvent> {
  override fun project(id: Int, event: CustomerEvent): Future<Void> {
    return when (event) {
      is CustomerRegistered -> repo.upsert(id, event.name, false)
      is CustomerActivated -> repo.updateStatus(id, true)
      is CustomerDeactivated -> repo.updateStatus(id, false)
    }
  }
}

/**
 * Read model repository
 */
class CustomerRepository(private val pool: PgPool) {
  fun upsert(id: Int, name: String, isActive: Boolean): Future<Void> {
    return toVoid(
      pool.withTransaction {
        it
          .preparedQuery(
            "INSERT INTO customers (id, name, is_active) VALUES ($1, $2, $3) ON CONFLICT DO UPDATE " +
              "SET name = $2, is_active = 3"
          )
          .execute(Tuple.of(id, name, isActive))
      }
    )
  }
  fun updateStatus(id: Int, isActive: Boolean): Future<Void> {
    return toVoid(
      pool.withTransaction {
        it
          .preparedQuery("UPDATE customers set isActive = $2 where id = $1")
          .execute(Tuple.of(id))
      }
    )
  }
}

/**
 * Just a boilerplate
 */
fun <T> toVoid(f1: Future<T>): Future<Void> {
  val promise = Promise.promise<Void>()
  f1.onFailure { promise.fail(it.cause) }
    .onSuccess { promise.complete() }
  return promise.future()
}
