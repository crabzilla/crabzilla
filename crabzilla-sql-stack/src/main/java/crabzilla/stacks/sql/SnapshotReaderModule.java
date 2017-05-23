package crabzilla.stacks.sql;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.CommandValidatorFn;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotReaderFn;

import java.util.function.Function;
import java.util.function.Supplier;

public interface SnapshotReaderModule<A extends AggregateRoot> {

  SnapshotReaderFn<A> snapshotReader(final Cache<String, Snapshot<A>> cache,
                                     final EventRepository eventRepo,
                                     final SnapshotFactory<A> snapshotFactory);

  Cache<String,  Snapshot<A>> cache();

  Supplier<Function<A, A>> depInjectionFnSupplier(final Function<A, A> depInjectionFn);

  Supplier<CommandValidatorFn> depInjectionFnSupplier(CommandValidatorFn cmdValidator);

}
