package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

typealias CustomerId = Int

// events

@Serializable
data class CustomerCreated(val customerId: CustomerId, val name: String) : DomainEvent()

@Serializable
data class CustomerActivated(val reason: String) : DomainEvent()

@Serializable
data class CustomerDeactivated(val reason: String) : DomainEvent()

// commands

@Serializable
data class CreateCustomer(val name: String) : Command()

@Serializable
data class ActivateCustomer(val reason: String) : Command()

@Serializable
data class DeactivateCustomer(val reason: String) : Command()

@Serializable
data class CreateActivateCustomer(val name: String, val reason: String) : Command()

@Serializable
data class UnknownCommand(val x: Int) : Command()

// kotlinx.serialization

val customerModule = SerializersModule {
  polymorphic(Entity::class) {
    Customer::class with Customer.serializer()
  }
  polymorphic(Command::class) {
    CreateCustomer::class with CreateCustomer.serializer()
    ActivateCustomer::class with ActivateCustomer.serializer()
    DeactivateCustomer::class with DeactivateCustomer.serializer()
    CreateActivateCustomer::class with CreateActivateCustomer.serializer()
    UnknownCommand::class with UnknownCommand.serializer()
  }
  polymorphic(DomainEvent::class) {
    CustomerCreated::class with CustomerCreated.serializer()
    CustomerActivated::class with CustomerActivated.serializer()
    CustomerDeactivated::class with CustomerDeactivated.serializer()
  }
}
