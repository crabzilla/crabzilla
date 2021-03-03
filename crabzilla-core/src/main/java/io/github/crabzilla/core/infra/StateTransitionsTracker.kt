package io.github.crabzilla.core.infra

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.DomainEvent

// object EventBusChannels {
//  val aggregateRootChannel = { entityName: String -> "crabzilla.aggregate.$entityName" }
//  fun streamChannel(entityName: String, streamId: String): String {
//    return "crabzilla.stream.$entityName.$streamId"
//  }
// }
//
// data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)
//
// class CrabzillaContext(
//  val json: Json,
//  val uowRepository: UnitOfWorkRepository,
//  val uowJournal: UnitOfWorkJournal
// )
//

class StateTransitionsTracker<A : AggregateRoot>(originalState: A, private val stateFn: (DomainEvent, A) -> A) {
  val appliedEvents = mutableListOf<DomainEvent>()
  var currentState: A = originalState
  fun applyEvents(events: List<DomainEvent>): StateTransitionsTracker<A> {
    events.forEach { domainEvent ->
      currentState = stateFn.invoke(domainEvent, currentState)
      appliedEvents.add(domainEvent)
    }
    return this
  }
  inline fun applyEvents(fn: (A) -> List<DomainEvent>): StateTransitionsTracker<A> {
    val newEvents = fn.invoke(currentState)
    return applyEvents(newEvents)
  }
}
