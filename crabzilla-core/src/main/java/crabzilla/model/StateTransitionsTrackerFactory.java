package crabzilla.model;

public interface StateTransitionsTrackerFactory<A extends AggregateRoot> {

  StateTransitionsTracker<A> create(A instance) ;

}
