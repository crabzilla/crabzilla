package crabzilla.stack;

import crabzilla.model.AggregateRoot;

@FunctionalInterface
public interface SnapshotReaderFn<A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final String id);

}