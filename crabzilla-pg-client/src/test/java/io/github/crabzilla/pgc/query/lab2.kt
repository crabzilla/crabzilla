package io.github.crabzilla.pgc.query

import io.github.crabzilla.pgc.query.CustomerEvent.CustomerCanceled
import io.github.crabzilla.pgc.query.CustomerEvent.CustomerRegistered

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
class CustomerRepository {
  fun register(name: String) { println("""sql to project "registered" event to read model database""") }
  fun cancel(name: String, reason: String) { println("""sql to project "canceled" event to read model database""") }
}
val repository = CustomerRepository()
fun projectEventToReadModel(event: DomainEvent) {
  when (event) {
    is CustomerRegistered -> repository.register(event.name)
    is CustomerCanceled -> repository.cancel(event.name, event.reason)
    else -> {}
  }
}
fun main() {
  val events: List<DomainEvent> = listOf(
    CustomerRegistered(mapOf(Pair("name", "customer-1"))),
    CustomerCanceled(mapOf(Pair("name", "customer-1"), Pair("reason", "bad credit"))))
  // given a list of events, project them to read model database
  events.forEach { event -> projectEventToReadModel(event) }
  // will print
  // sql to project "registered" event to read model database
  // sql to project "canceled" event to read model database
}
