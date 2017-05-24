package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.Event;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootModule<A extends AggregateRoot> {

  Supplier<A> supplier() ;

  BiFunction<Event, A, A> stateTransitionFn() ;

  SnapshotFactory<A> snapshotFactory(Supplier<A> supplier,
                                     Function<A, A> depInjectionFn,
                                     BiFunction<Event, A, A> stateTransFn) ;

  CommandHandlerFn<A> cmdHandler(final BiFunction<Event, A, A> stateTransFn,
                                 final Function<A, A> depInjectionFn) ;

}
