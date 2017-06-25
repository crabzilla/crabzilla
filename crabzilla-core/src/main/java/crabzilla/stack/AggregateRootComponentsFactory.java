package crabzilla.stack;

import crabzilla.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootComponentsFactory<A extends AggregateRoot> {

  Supplier<A> supplierFn() ;

  Function<A, A> depInjectionFn() ;

  BiFunction<Event, A, A> stateTransitionFn() ;

  Function<Command, List<String>> cmdValidatorFn() ;

  BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn() ;

  default SnapshotFactory<A> snaphotFactory() {
    return new SnapshotFactory<>(supplierFn(), depInjectionFn(), stateTransitionFn());
  }

}
