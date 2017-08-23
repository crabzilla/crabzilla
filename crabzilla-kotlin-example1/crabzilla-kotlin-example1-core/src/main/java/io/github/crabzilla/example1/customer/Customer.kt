package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.example1.eventsOf
import io.github.crabzilla.model.Aggregate
import io.github.crabzilla.model.DomainEvent
import io.github.crabzilla.model.EntityId

// tag::aggregate[]

data class Customer(val _id: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null,
                    val sampleInternalService: SampleInternalService) : Aggregate {

  override fun getId(): EntityId {
    return _id!!
  }

  internal fun create(id: CustomerId, name: String): List<DomainEvent> {
    require(this._id == null, { "customer already created" })
    return eventsOf(CustomerCreated(id, name))
  }

  internal fun activate(reason: String): List<DomainEvent> {
    require(this._id != null, { "customer must exists" })
    return eventsOf(CustomerActivated(reason, sampleInternalService.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    require(this._id != null, { "customer must exists" })
    return eventsOf(CustomerDeactivated(reason, sampleInternalService.now()))
  }

}


// end::aggregate[]
