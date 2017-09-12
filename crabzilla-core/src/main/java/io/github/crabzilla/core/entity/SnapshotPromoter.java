package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

@Slf4j
public class SnapshotPromoter<A extends Entity> {

  private final StateTransitionsTrackerFactory<A> trackerFactory;

  public SnapshotPromoter(StateTransitionsTrackerFactory<A> trackerFactory) {
    this.trackerFactory = trackerFactory;
  }

  public Snapshot<A> promote(Snapshot<A> originalSnapshot, Version newVersion, List<DomainEvent> newEvents) {

    if (originalSnapshot.getVersion().getValueAsLong() != newVersion.getValueAsLong() -1) {
      throw new RuntimeException(String.format("Cannot upgrade to version %s since current version is %s",
              newVersion, originalSnapshot.getVersion()));
    }

    val tracker = trackerFactory.apply(originalSnapshot);

    return new Snapshot<>(tracker.applyEvents(c -> newEvents).currentState(), newVersion);
  }

}
