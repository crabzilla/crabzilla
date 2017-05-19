package crabzilla.model;

import crabzilla.UnitOfWork;
import crabzilla.Version;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AggregateRootCmdHandler<A extends AggregateRoot> {

  protected final BiFunction<Event, A, A> stateTransitionFn;
  protected final Function<A, A> dependencyInjectionFn;

  protected AggregateRootCmdHandler(@NonNull BiFunction<Event, A, A> stateTransitionFn,
                                    @NonNull Function<A, A> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
  }

  public abstract Optional<UnitOfWork> handle(Command cmd, A targetInstance, Version targetVersion);

}
