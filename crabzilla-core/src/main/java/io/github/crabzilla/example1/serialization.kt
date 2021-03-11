package io.github.crabzilla.example1

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.javaModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * kotlinx.serialization
 */
@kotlinx.serialization.ExperimentalSerializationApi
val customerModule = SerializersModule {
  include(javaModule)
  polymorphic(AggregateRoot::class) {
    subclass(Customer::class, Customer.serializer())
  }
  polymorphic(Command::class) {
    subclass(CustomerCommand.RegisterCustomer::class, CustomerCommand.RegisterCustomer.serializer())
    subclass(CustomerCommand.ActivateCustomer::class, CustomerCommand.ActivateCustomer.serializer())
    subclass(CustomerCommand.DeactivateCustomer::class, CustomerCommand.DeactivateCustomer.serializer())
    subclass(CustomerCommand.RegisterAndActivateCustomer::class, CustomerCommand.RegisterAndActivateCustomer.serializer())
  }
  polymorphic(DomainEvent::class) {
    subclass(CustomerEvent.CustomerRegistered::class, CustomerEvent.CustomerRegistered.serializer())
    subclass(CustomerEvent.CustomerActivated::class, CustomerEvent.CustomerActivated.serializer())
    subclass(CustomerEvent.CustomerDeactivated::class, CustomerEvent.CustomerDeactivated.serializer())
  }
}

val customerJson = Json { serializersModule = customerModule }

@kotlinx.serialization.ExperimentalSerializationApi
fun main() {

  val json = Json { serializersModule = customerModule }

  val pair = Customer.create(id = 1, name = "c1")

  val c = pair.state

  println(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  c.activate("don't ask!")
  println(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, c))

  val cmd = CustomerCommand.RegisterAndActivateCustomer(customerId = 1, name = "c1", reason = "i can")
  println(json.encodeToString(COMMAND_SERIALIZER, cmd))

  val event = CustomerEvent.CustomerRegistered(id = 1, name = "c1")
  println(customerEventSer.toJson(event))

  val event2 = CustomerEvent.CustomerActivated("because I can")
  println(customerEventSer.toJson(event2))
}
