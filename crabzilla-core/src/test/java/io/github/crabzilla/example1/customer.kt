package io.github.crabzilla.example1

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventDeserializer
import io.github.crabzilla.core.EventSerializer
import io.github.crabzilla.core.IntegrationEvent
import io.github.crabzilla.core.Try
import io.github.crabzilla.core.javaModule
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerCreated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

typealias CustomerId = Int

sealed class CustomerEvent : DomainEvent() {
  class CustomerCreated(val customerId: Int, val name: String) : CustomerEvent()
  class CustomerActivated(val reason: String) : CustomerEvent()
  class CustomerDeactivated(val reason: String) : CustomerEvent()
}

class CustomerEventSer : EventSerializer<CustomerEvent> {
  override fun toJson(e: CustomerEvent): Try<JsonObject> {
    return Try { when (e) {
      is CustomerCreated -> jsonObjectOf(Pair("companyId", e.customerId), Pair("name", e.name))
      is CustomerActivated -> jsonObjectOf(Pair("reason", e.reason))
      is CustomerDeactivated -> jsonObjectOf(Pair("reason", e.reason))
    } }
  }
}

@Serializable
sealed class CustomerCommand : Command() {
  @Serializable
  data class CreateCustomer(val customerId: CustomerId, val name: String) : CustomerCommand()
  @Serializable
  data class ActivateCustomer(val reason: String) : CustomerCommand()
  @Serializable
  data class DeactivateCustomer(val reason: String) : CustomerCommand()
  @Serializable
  data class CreateActivateCustomer(val customerId: CustomerId, val name: String, val reason: String) : CustomerCommand()
}

@Serializable
data class Customer(
  val customerId: CustomerId,
  var name: String? = null,
  var isActive: Boolean = false,
  var reason: String? = null
) : AggregateRoot() {

  companion object {
    // for command handler use
  }

  constructor(id: CustomerId) : this(customerId = id) {

  }

  fun activate(reason: String) {
    apply(CustomerActivated(reason))
  }

  fun deactivate(reason: String) {
    apply(CustomerDeactivated(reason))
  }

  override fun id(): String {
    return customerId.toString()
  }

  override fun applyEvent(e: DomainEvent) {
    when (e) {
      is CustomerCreated -> {
        customerId = e.customerId
        name = e.name
        isActive = false
      }
      is CustomerActivated -> {
        isActive = true
        reason = e.reason
      }
      is CustomerDeactivated -> {
        isActive = false
        reason = e.reason
      }
    }
  }
}

val customerModule = SerializersModule {
  include(javaModule)
  polymorphic(AggregateRoot::class) {
    subclass(Customer::class, Customer.serializer())
  }
  polymorphic(Command::class) {
    subclass(CustomerCommand.CreateCustomer::class, CustomerCommand.CreateCustomer.serializer())
    subclass(CustomerCommand.ActivateCustomer::class, CustomerCommand.ActivateCustomer.serializer())
    subclass(CustomerCommand.DeactivateCustomer::class, CustomerCommand.DeactivateCustomer.serializer())
    subclass(CustomerCommand.CreateActivateCustomer::class, CustomerCommand.CreateActivateCustomer.serializer())
  }
}

fun main() {

  val json = Json { serializersModule = customerModule }

  val c = Customer(customerId = 1, name = "c1")
  println(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  c.activate("don't ask!")
  println(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  val cmd = CustomerCommand.CreateActivateCustomer(customerId = 1, name = "c1", reason = "i can")
  println(json.encodeToString(COMMAND_SERIALIZER, cmd))

  // ex2

  val ser = CustomerEventSer()

  val event = CustomerCreated(mapOf(Pair("customerId", 1), Pair("name", "customer#1")))
  println(ser.toJson(event))

  val event2 = CustomerActivated(mapOf(Pair("reason", "because I can")))
  println(ser.toJson(event2))
}

// command aware

// class CustomerCommandAware : AggregateRootCommandAware<CustomerCommand, Customer, CustomerEvent> {
//
//  override val entityName = "customer"
//
//  override fun initialState(event: CustomerEvent): Customer? {
//    val c = when (event) {
//      is CustomerCreated -> Customer(event.customerId, event.name)
//      else -> null
//    }
//    c?.apply(event)
//    return c
//  }
//
//  override fun validateCmd(command: CustomerCommand): List<String> {
//    return when (command) {
//      is CreateCustomer ->
//        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
//      is CreateActivateCustomer ->
//        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
//      is ActivateCustomer, is DeactivateCustomer -> listOf()
//      else -> listOf("invalid command ${command.javaClass.simpleName}") // all other commands are invalid
//    }
//  }
//
//  override fun handleCmd(id: Int, customer: Customer, command: CustomerCommand) {
//    when (command) {
//      is CreateCustomer -> customer.create(id, command.name)
//      is ActivateCustomer -> succeededFuture(customer.activate(command.reason))
//      is DeactivateCustomer -> customer.deactivate(command.reason)
//      is CreateActivateCustomer -> createActivate(id, customer, command.name, command.reason)
//      else -> println("${command::class.java} is a unknown command")
//    }
//  }
// }

// }
