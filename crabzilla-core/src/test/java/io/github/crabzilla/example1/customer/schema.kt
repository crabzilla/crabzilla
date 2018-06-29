package io.github.crabzilla.example1.customer

import io.github.crabzilla.Command
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.EntityId
import java.time.Instant
import java.util.*

data class CustomerId(public val id: Int) : EntityId {
  override fun value(): Int {
    return id
  }
}

// events

data class CustomerCreated(val id: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// commands

data class CreateCustomer(override val commandId: UUID, override val targetId: CustomerId, val name: String) : Command

data class ActivateCustomer(override val commandId: UUID, override val targetId: CustomerId,
                            val reason: String) : Command

data class DeactivateCustomer(override val commandId: UUID, override val targetId: CustomerId,
                              val reason: String) : Command

data class CreateActivateCustomer(override val commandId: UUID,
                                  override val targetId: CustomerId,
                                  val name: String, val reason: String) : Command

// just for test

data class UnknownCommand(override val commandId: UUID, override val targetId: CustomerId)
  : Command
