package crabzilla.example1.customer

import crabzilla.example1.SampleInternalService
import crabzilla.example1.eventsOf
import crabzilla.model.Aggregate
import crabzilla.model.DomainEvent
import crabzilla.model.EntityId

data class Customer(val _id: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null,
                    val sampleInternalService: SampleInternalService? = null) : Aggregate {

  override fun getId(): EntityId {
    return _id!!
  }

  internal fun create(id: CustomerId, name: String): List<DomainEvent> {
    require(this._id == null, { "customer already created" })
    return eventsOf(CustomerCreated(id, name))
  }

  internal fun activate(reason: String): List<DomainEvent> {
    return eventsOf(CustomerActivated(reason, sampleInternalService!!.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    return eventsOf(CustomerDeactivated(reason, sampleInternalService!!.now()))
  }

}
