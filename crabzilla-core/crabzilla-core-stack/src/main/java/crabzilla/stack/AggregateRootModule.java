package crabzilla.stack;

import crabzilla.model.*;
import crabzilla.model.util.Either;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootModule<A extends AggregateRoot> {

  Supplier<A> supplier() ;

  BiFunction<Event, A, A> stateTransitionFn() ;

  BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn(
          Function<A, A> depInjectionFn, BiFunction<Event, A, A> stateTransFn) ;

  SnapshotFactory<A> snapshotFactory(Supplier<A> supplier,
                                     Function<A, A> depInjectionFn,
                                     BiFunction<Event, A, A> stateTransitionFn);

}
