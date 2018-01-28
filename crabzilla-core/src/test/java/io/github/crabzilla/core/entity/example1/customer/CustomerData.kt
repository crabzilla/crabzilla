package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import java.time.Instant
import java.util.*

data class CustomerId(val id: String) : EntityId {
  override fun stringValue(): String {
    return id
  }
}

// tag::events[]

data class CustomerCreated(val id: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// end::events[]

// tag::commands[]

data class CreateCustomer(override val commandId: UUID, override val targetId: CustomerId,
                          val name: String) : EntityCommand

data class ActivateCustomer(override val commandId: UUID, override val targetId: CustomerId,
                            val reason: String) : EntityCommand

data class DeactivateCustomer(override val commandId: UUID, override val targetId: CustomerId,
                              val reason: String) : EntityCommand

data class CreateActivateCustomer(override val commandId: UUID,
                                  override val targetId: CustomerId,
                                  val name: String, val reason: String) : EntityCommand

// end::commands[]

data class UnknownCommand(override val commandId: UUID, override val targetId: CustomerId)
  : EntityCommand
