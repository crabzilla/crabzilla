package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import crabzilla.model.AggregateRootId;

@FunctionalInterface
public interface SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final ID id);

}