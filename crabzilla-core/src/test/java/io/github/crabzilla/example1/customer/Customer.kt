package io.github.crabzilla.example1.customer

import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.Entity
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
  val customerId: CustomerId? = null,
  val name: String? = null,
  val isActive: Boolean? = false,
  val reason: String? = null
) : Entity() {

  // TODO perform a query on read model to validate name uniqueness
  fun create(id: CustomerId, name: String): Future<List<DomainEvent>> {
    require(this.customerId == null) { "customer already created" }
    return succeededFuture(eventsOf(CustomerCreated(id, name)))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason))
  }

  // TODO perform a web request ask if this operation is allowed
  fun deactivate(reason: String): Future<List<DomainEvent>> {
    customerMustExist()
    return succeededFuture(eventsOf(CustomerDeactivated(reason)))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }
}
