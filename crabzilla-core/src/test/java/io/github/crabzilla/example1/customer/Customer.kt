package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.*

internal data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null,
                    val sampleInternalService: SampleInternalService) : Entity {

  override val id: EntityId
    get() = customerId!!

  internal fun create(id: CustomerId, name: String): List<DomainEvent> {
    require(this.customerId == null, { "customer already created" })
    return eventsOf(CustomerCreated(id, name))
  }

  internal fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, sampleInternalService.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerDeactivated(reason, sampleInternalService.now()))
  }

  private fun customerMustExist() {
    require(this.customerId != null, { "customer must exists" })
  }

}
