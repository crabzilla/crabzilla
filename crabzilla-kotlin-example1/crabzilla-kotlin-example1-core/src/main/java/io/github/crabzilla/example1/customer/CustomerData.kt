package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.KotlinEntityCommand
import io.github.crabzilla.model.DomainEvent
import io.github.crabzilla.model.EntityId
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

data class CreateCustomer(override val _commandId: UUID, override val _targetId: CustomerId,
                          val name: String) : KotlinEntityCommand

data class ActivateCustomer(override val _commandId: UUID, override val _targetId: CustomerId,
                            val reason: String) : KotlinEntityCommand

data class DeactivateCustomer(override val _commandId: UUID, override val _targetId: CustomerId,
                              val reason: String) : KotlinEntityCommand

data class CreateActivateCustomer(override val _commandId: UUID,
                                  override val _targetId: CustomerId,
                                  val name: String, val reason: String) : KotlinEntityCommand

// end::commands[]

data class UnknownCommand(override val _commandId: UUID, override val _targetId: CustomerId)
    : KotlinEntityCommand

