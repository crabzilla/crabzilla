package io.github.crabzilla.pgc.command

import io.github.crabzilla.pgc.command.CustomerEvent.CustomerCanceled
import io.github.crabzilla.pgc.command.CustomerEvent.CustomerRegistered

data class Customer(val name: String? = null, val status: String? = null) // the "aggregate root"
interface DomainEvent
sealed class CustomerEvent : DomainEvent {
  class CustomerRegistered(val map: Map<String, Any?>) : CustomerEvent() {
    val name: String by map
  }
  class CustomerCanceled(val map: Map<String, Any?>) : CustomerEvent() {
    val name: String by map
    val reason: String by map
  }
}
fun applyEvent(event: DomainEvent, customer: Customer): Customer {
  return when (event) {
    is CustomerRegistered -> customer.copy(name = event.name, status = "active")
    is CustomerCanceled -> customer.copy(status = "canceled")
    else -> customer
  }
}
// don't forget you can optimize it a lot with snapshots, for instance a Pair<Customer, Version>
fun main() {
  val events: List<DomainEvent> = listOf(
    CustomerRegistered(mapOf("name" to "customer-1")),
    CustomerCanceled(mapOf("name" to "customer-1", "reason" to "bad credit")))
  val customer = events.fold(Customer(), { state, event -> applyEvent(event, state) })
  println(customer) // will print Customer(name=customer-1, status=canceled)
}
