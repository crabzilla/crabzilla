package io.github.crabzilla.example1.customer.effects

import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.writer.StateEffect
import io.vertx.core.Future
import java.util.*

class CustomerStateEffect(private val cache: LoadingCache<UUID, Customer>) : StateEffect<Customer> {
  override fun handle(
    id: String,
    state: Customer,
  ): Future<Void> {
    return try {
      cache.put(UUID.fromString(id), state)
      Future.succeededFuture()
    } catch (e: Exception) {
      Future.failedFuture(e)
    }
  }
}
