package crabzilla.model;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class SnapshotPromoter<A extends Aggregate> {

  final Supplier<A> supplier ;
  final StateTransitionsTrackerFactory<A> trackerFactory;

  public SnapshotPromoter(Supplier<A> supplier, StateTransitionsTrackerFactory<A> trackerFactory) {
    this.supplier = supplier;
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
