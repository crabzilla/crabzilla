package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EntityId
import io.github.crabzilla.core.entity.Entity
import io.github.crabzilla.core.eventsOf
import io.github.crabzilla.example1.SampleInternalService

// tag::aggregate[]

data class Customer(val customerId: CustomerId? = null,
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
    require(this.customerId != null, { "customer must exists" })
    return eventsOf(CustomerActivated(reason, sampleInternalService.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    require(this.customerId != null, { "customer must exists" })
    return eventsOf(CustomerDeactivated(reason, sampleInternalService.now()))
  }

}


// end::aggregate[]
