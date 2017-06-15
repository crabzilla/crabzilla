package crabzilla.stack;

import crabzilla.model.*;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class SnapshotFactory<A extends AggregateRoot> {

  final Supplier<A> supplier;
  final Function<A, A> dependencyInjectionFn;
  final BiFunction<Event, A, A> stateTransitionFn;
  final Snapshot<A> EMPTY_SNAPSHOT ;

  @Inject
  public SnapshotFactory(Supplier<A> supplier, Function<A, A> dependencyInjectionFn, BiFunction<Event, A, A> stateTransitionFn) {
    this.supplier = supplier;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.stateTransitionFn = stateTransitionFn;
    this.EMPTY_SNAPSHOT = new Snapshot<>(supplier.get(), new Version(0));
  }

  public Snapshot<A> createSnapshot(EventRepository.SnapshotData snapshotData) {

    return applyNewEventsToSnapshot(EMPTY_SNAPSHOT, snapshotData.getVersion(), snapshotData.getEvents());
  }

  public Snapshot<A> applyNewEventsToSnapshot(Snapshot<A> originalSnapshot, Version newVersion, List<Event> newEvents) {

    if (originalSnapshot.getVersion().getValueAsLong() >= newVersion.getValueAsLong()) {
      throw new RuntimeException(String.format("Cannot upgrade to version %s since my version is %s",
              newVersion, originalSnapshot.getVersion()));
    }

    val tracker = new StateTransitionsTracker<A>(originalSnapshot.getInstance(),
            stateTransitionFn, dependencyInjectionFn);

    return new Snapshot<>(tracker.applyEvents(c -> newEvents).currentState(), newVersion);
  }

  public Snapshot<A> getEmptySnapshot() {

    return EMPTY_SNAPSHOT;
  }

}
