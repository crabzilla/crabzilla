package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import crabzilla.model.AggregateRootCmdHandler;
import crabzilla.model.Command;
import crabzilla.model.Event;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootModule<A extends AggregateRoot> {

  Supplier<A> supplier() ;

  BiFunction<Event, A, A> stateTransitionFn() ;
  
  AggregateRootCmdHandler<A> cmdHandler(final BiFunction<Event, A, A> stateTransFn,
                                        final Function<A, A> depInjectionFn) ;

  Function<Event, Optional<Command>> eventMonitoringFn() ;

  Supplier<Function<A, A>> depInjectionFnSupplier(final Function<A, A> depInjectionFn);

}
