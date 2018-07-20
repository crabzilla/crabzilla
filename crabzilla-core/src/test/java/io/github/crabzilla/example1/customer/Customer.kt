package io.github.crabzilla.example1.customer

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.Entity
import io.github.crabzilla.EntityId
import io.github.crabzilla.eventsOf
import io.github.crabzilla.example1.PojoService

internal data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null,
                    val pojoService: PojoService) : Entity {

  override val id: EntityId?
    get() = customerId

  internal fun create(id: CustomerId, name: String): List<DomainEvent> {
    require(this.customerId == null, { "customer already created" })
    return eventsOf(CustomerCreated(id, name))
  }

  internal fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, pojoService.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerDeactivated(reason, pojoService.now()))
  }

  private fun customerMustExist() {
    require(this.customerId != null, { "customer must exists" })
  }

}
