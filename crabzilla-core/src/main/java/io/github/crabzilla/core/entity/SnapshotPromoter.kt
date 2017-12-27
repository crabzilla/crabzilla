package io.github.crabzilla.core.entity

import io.github.crabzilla.core.DomainEvent

open class SnapshotPromoter<A : Entity>(private val trackerFactory: (Snapshot<A>) -> StateTransitionsTracker<A>) {

  fun promote(originalSnapshot: Snapshot<A>, newVersion: Version, newEvents: List<DomainEvent>): Snapshot<A> {

    if (originalSnapshot.version.valueAsLong != newVersion.valueAsLong - 1) {
      throw RuntimeException(String.format("Cannot upgrade to version %s since current version is %s",
              newVersion, originalSnapshot.version))
    }

    val tracker: StateTransitionsTracker<A> = trackerFactory.invoke(originalSnapshot)

    return Snapshot(tracker.applyEvents({ newEvents }).currentState(), newVersion)
  }

}
