package io.github.crabzilla.core.entity;

import java.util.function.Function;

public interface StateTransitionsTrackerFactory<A extends Entity>
        extends Function<Snapshot<A>, StateTransitionsTracker<A>> {

}
