package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.Entity
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.example1.CustomerId
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import java.time.Instant

data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null) : Entity {

  // TODO perform a query on read model to validate name uniqueness
  fun create(id: CustomerId, name: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
    require(this.customerId == null) { "customer already created" }
    handler.handle(Future.succeededFuture(eventsOf(CustomerCreated(id, name))))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, Instant.now()))
  }

  // TODO perform a web request ask if this operation is allowed
  fun deactivate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
    handler.handle(Future.future { event ->
      customerMustExist()
      event.complete(eventsOf(CustomerDeactivated(reason, Instant.now())))
    })
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }

}
