package io.github.crabzilla.example1

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventDeserializer
import io.github.crabzilla.core.EventSerializer
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

sealed class CustomerEvent : DomainEvent {
  class CustomerCreated(val map: Map<String, Any?>) : CustomerEvent() {
    val customerId: Int by map
    val name: String by map
  }
  class CustomerActivated(val map: Map<String, Any?>) : CustomerEvent() {
    val reason: String by map
  }
  class CustomerDeactivated(val map: Map<String, Any?>) : CustomerEvent() {
    val reason: String by map
  }
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

class CustomerEventDes : EventDeserializer<CustomerEvent> {
  override fun fromJson(type: String, j: JsonObject): Try<CustomerEvent> {
    return Try { when (type) {
      CustomerCreated::class.simpleName ->
        CustomerCreated(mapOf(Pair("companyId", j.getInteger("companyId")), Pair("name", j.getString("name"))))
      CustomerActivated::class.simpleName ->
        CustomerActivated(mapOf(Pair("reason", j.getString("reason"))))
      CustomerDeactivated::class.simpleName ->
        CustomerDeactivated(mapOf(Pair("reason", j.getString("reason"))))
      else -> throw IllegalArgumentException("Unknown event type: $type")
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
  var customerId: CustomerId,
  var name: String,
  var isActive: Boolean = false,
  var reason: String? = null
) : AggregateRoot() {

  fun activate(reason: String) {
    apply(CustomerActivated(mapOf(Pair("reason", reason))))
  }

  fun deactivate(reason: String) {
    apply(CustomerDeactivated(mapOf(Pair("reason", reason))))
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

val EXAMPLE1_JSON = Json { serializersModule = customerModule }

fun main() {

  val c = Customer(customerId = 1, name = "c1")
  println(EXAMPLE1_JSON.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  c.activate("don't ask!")
  println(EXAMPLE1_JSON.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  val cmd = CustomerCommand.CreateActivateCustomer(customerId = 1, name = "c1", reason = "i can")
  println(EXAMPLE1_JSON.encodeToString(COMMAND_SERIALIZER, cmd))

  // ex2

  val ser = CustomerEventSer()

  val event = CustomerCreated(mapOf(Pair("customerId", 1), Pair("name", "customer#1")))
  println(ser.toJson(event))

  val event2 = CustomerActivated(mapOf(Pair("reason", "because I can")))
  println(ser.toJson(event2))
}
