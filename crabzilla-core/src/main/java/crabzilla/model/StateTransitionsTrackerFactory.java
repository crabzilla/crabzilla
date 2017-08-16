package crabzilla.model;

import java.util.function.Function;

public interface StateTransitionsTrackerFactory<A extends Aggregate> extends Function<A, StateTransitionsTracker<A>> {

}
