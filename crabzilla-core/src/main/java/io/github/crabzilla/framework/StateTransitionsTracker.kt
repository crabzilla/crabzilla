package io.github.crabzilla.framework

class StateTransitionsTracker<A : Entity>(originalSnapshot: Snapshot<A>,
                                          private val applyEventsFn: (DomainEvent, A) -> A) {

  val appliedEvents = mutableListOf<DomainEvent>()
  var currentState: A = originalSnapshot.state

  fun applyEvents(events: List<DomainEvent>): StateTransitionsTracker<A> {
    events.forEach { domainEvent ->
      currentState = applyEventsFn.invoke(domainEvent, currentState)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun applyEvents(fn: (A) -> List<DomainEvent>): StateTransitionsTracker<A> {
    val newEvents = fn.invoke(currentState)
    return applyEvents(newEvents)
  }
}
