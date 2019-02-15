package io.github.crabzilla

import java.util.*

data class Snapshot<A : Entity>(val instance: A, val version: Version) {

  fun upgradeTo(newVersion: Version, newEvents: List<DomainEvent>,
              trackerFactory: (Snapshot<A>) -> StateTransitionsTracker<A>) : Snapshot<A> {

    if (version != newVersion-1) {
      throw RuntimeException(String.format("Cannot upgrade to version %s since current version is %s",
        newVersion, version))
    }

    val tracker: StateTransitionsTracker<A> = trackerFactory.invoke(this)

    return Snapshot(tracker.applyEvents { newEvents }.currentState(), newVersion)
  }
}

data class SnapshotData(val version: Version, val events: List<DomainEvent>)

class StateTransitionsTracker<A : Entity>(private val originalSnapshot: Snapshot<A>,
                                          private val applyEventsFn: (DomainEvent, A) -> A) {
  private val stateTransitions = ArrayList<StateTransition<A>>()

  inline fun applyEvents(aggregateRootMethodFn: (A) -> List<DomainEvent>): StateTransitionsTracker<A> {
    val newEvents = aggregateRootMethodFn.invoke(currentState())
    return applyEvents(newEvents)
  }

  fun collectEvents(): List<DomainEvent> {
    return stateTransitions.map { t -> t.afterThisEvent }.toList()
  }

  fun currentState(): A {
    return if (isEmpty) originalSnapshot.instance else stateTransitions[stateTransitions.size - 1].newInstance
  }

  val isEmpty: Boolean
    get() = stateTransitions.isEmpty()

  fun applyEvents(events: List<DomainEvent>): StateTransitionsTracker<A> {
    events.forEach { e ->
      val newInstance = applyEventsFn.invoke(e, currentState())
      stateTransitions.add(StateTransition(newInstance, e))
    }
    return this
  }
  internal inner class StateTransition<out T : Entity>(val newInstance: T, val afterThisEvent: DomainEvent)
}
