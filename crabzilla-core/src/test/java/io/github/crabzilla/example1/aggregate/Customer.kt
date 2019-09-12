package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.example1.CustomerId
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import java.time.Instant

data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null) : Entity {

  // TODO perform a query on read model to validate name uniqueness
  fun create(id: CustomerId, name: String) : Promise<List<DomainEvent>> {
    require(this.customerId == null) { "customer already created" }
    return Promise.succeededPromise(eventsOf(CustomerCreated(id, name)))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, Instant.now()))
  }

  // TODO perform a web request ask if this operation is allowed
  fun deactivate(reason: String) : Promise<List<DomainEvent>> {
    customerMustExist()
    return Promise.succeededPromise(eventsOf(CustomerDeactivated(reason, Instant.now())))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }

}
