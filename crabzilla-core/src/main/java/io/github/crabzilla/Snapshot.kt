package io.github.crabzilla

data class Snapshot<A : Entity>(val instance: A, val version: Version) {

  fun upgradeTo(newVersion: Version, newEvents: List<DomainEvent>,
                applyEventsFn: (DomainEvent, A) -> A) : Snapshot<A> {

    if (version != newVersion-1) {
      throw RuntimeException(String.format("Cannot upgrade to version %s since current version is %s",
        newVersion, version))
    }

    val tracker: StateTransitionsTracker<A> = StateTransitionsTracker(this, applyEventsFn)

    return Snapshot(tracker.applyEvents { newEvents }.currentState(), newVersion)
  }
}
