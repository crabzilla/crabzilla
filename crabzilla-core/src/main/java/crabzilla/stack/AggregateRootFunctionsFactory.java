package crabzilla.stack;

import crabzilla.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootFunctionsFactory<A extends AggregateRoot> {

  Supplier<A> supplierFn() ;

  Function<A, A> depInjectionFn() ;

  BiFunction<DomainEvent, A, A> stateTransitionFn() ;

  Function<EntityCommand, List<String>> cmdValidatorFn() ;

  BiFunction<EntityCommand, Snapshot<A>, Either<Throwable, Optional<EntityUnitOfWork>>> cmdHandlerFn() ;

}
