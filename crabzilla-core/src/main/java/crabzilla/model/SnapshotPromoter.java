package crabzilla.model;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class SnapshotPromoter<A extends AggregateRoot> {

  final Supplier<A> supplier ;
  final StateTransitionsTrackerFactory<A> trackerFactory;
  final Snapshot<A> EMPTY_SNAPSHOT ;


  @Inject
  public SnapshotPromoter(Supplier<A> supplier, StateTransitionsTrackerFactory<A> trackerFactory) {
    this.supplier = supplier;
    this.trackerFactory = trackerFactory;
    this.EMPTY_SNAPSHOT = new Snapshot<>(supplier.get(), new Version(0));
  }

  public Snapshot<A> getEmptySnapshot() {

    return EMPTY_SNAPSHOT;
  }

  public Snapshot<A> promote(Snapshot<A> originalSnapshot, Version newVersion, List<DomainEvent> newEvents) {

    if (originalSnapshot.getVersion().getValueAsLong() != newVersion.getValueAsLong() -1) {
      throw new RuntimeException(String.format("Cannot upgrade to version %s since current version is %s",
              newVersion, originalSnapshot.getVersion()));
    }

    val tracker = trackerFactory.create(originalSnapshot.getInstance());

    return new Snapshot<>(tracker.applyEvents(c -> newEvents).currentState(), newVersion);
  }

}
