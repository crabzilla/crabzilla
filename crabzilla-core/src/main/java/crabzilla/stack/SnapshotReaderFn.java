package crabzilla.stack;

import crabzilla.model.AggregateRoot;

@FunctionalInterface
public interface SnapshotReaderFn<A extends AggregateRoot> {

	SnapshotMessage<A> getSnapshotMessage(final String id);

}