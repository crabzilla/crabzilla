package io.github.crabzilla.example1.customer

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.example1.KotlinEntityCommand
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


data class CreateCustomer(@JsonProperty("commandId") override val _commandId: UUID,
                          @JsonProperty("targetId") override val _targetId: CustomerId,
                          val name: String) : KotlinEntityCommand

data class ActivateCustomer(@JsonProperty("commandId") override val _commandId: UUID,
                            @JsonProperty("targetId") override val _targetId: CustomerId,
                            val reason: String) : KotlinEntityCommand

data class DeactivateCustomer(@JsonProperty("commandId") override val _commandId: UUID,
                              @JsonProperty("targetId") override val _targetId: CustomerId,
                              val reason: String) : KotlinEntityCommand

data class CreateActivateCustomer(@JsonProperty("commandId") override val _commandId: UUID,
                                  @JsonProperty("targetId") override val _targetId: CustomerId,
                                  val name: String, val reason: String) : KotlinEntityCommand

// end::commands[]

data class UnknownCommand(@JsonProperty("commandId") override val _commandId: UUID,
                          @JsonProperty("targetId") override val _targetId: CustomerId)
    : KotlinEntityCommand

