package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.CommandValidatorFn;
import crabzilla.model.Event;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootModule<A extends AggregateRoot> {

  Supplier<A> supplier() ;

  BiFunction<Event, A, A> stateTransitionFn() ;

  SnapshotFactory<A> snapshotFactory(Supplier<A> supplier,
                                     Supplier<Function<A, A>> depInjectionFn,
                                     BiFunction<Event, A, A> stateTransFn) ;

  CommandHandlerFn<A> cmdHandlerFn(final BiFunction<Event, A, A> stateTransFn,
                                   final Supplier<Function<A, A>> depInjectionFn) ;

  // next Supplier methods are only a way to force submodules to provide these suppliers results

  Supplier<Function<A, A>> depInjectionFnSupplier(final Function<A, A> depInjectionFn);

  Supplier<CommandValidatorFn> cmdValidatorSupplier(CommandValidatorFn cmdValidator);

  Supplier<SnapshotReaderFn<A>> snapshotReaderFn(final SnapshotReaderFn<A> snapshotReaderFn);

}
