package crabzilla.stacks.sql;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotReaderFn;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SnapshotReaderModule<A extends AggregateRoot> {

  public SnapshotReaderFn<A> snapshotReader(final Cache<String, Snapshot<A>> cache,
                                            final EventRepository eventRepo,
                                            final Supplier<A> supplier,
                                            final Function<A, A> dependencyInjectionFn,
                                            final BiFunction<Event, A, A> stateTransitionFn);

  public Cache<String, Snapshot<A>> cache();

  Supplier<Function<A, A>> depInjectionFnSupplier(final Function<A, A> depInjectionFn);

}
