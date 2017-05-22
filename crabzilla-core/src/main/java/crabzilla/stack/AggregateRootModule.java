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
  
  CommandHandlerFn<A> cmdHandler(final BiFunction<Event, A, A> stateTransFn,
                                 final Function<A, A> depInjectionFn) ;


}
