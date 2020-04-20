package io.github.crabzilla.example1.customer

import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.succeededPromise
import io.vertx.core.Promise
import java.time.Instant

data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null) : Entity {

  // TODO perform a query on read model to validate name uniqueness
  fun create(id: CustomerId, name: String) : Promise<List<DomainEvent>> {
    require(this.customerId == null) { "customer already created" }
    return succeededPromise(eventsOf(CustomerCreated(id, name)))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, Instant.now()))
  }

  // TODO perform a web request ask if this operation is allowed
  fun deactivate(reason: String) : Promise<List<DomainEvent>> {
    customerMustExist()
    return succeededPromise(eventsOf(CustomerDeactivated(reason, Instant.now())))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }

}
