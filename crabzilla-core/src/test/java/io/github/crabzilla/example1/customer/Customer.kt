package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
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

  fun create(id: CustomerId, name: String): Future<List<DomainEvent>> {
    require(this.customerId == null) { "customer already created" }
    return succeededFuture(listOf(CustomerCreated(id, name)))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return listOf(CustomerActivated(reason))
  }

  fun deactivate(reason: String): Future<List<DomainEvent>> {
    customerMustExist()
    return succeededFuture(listOf(CustomerDeactivated(reason)))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }
}
