package crabzilla.model;

import crabzilla.UnitOfWork;
import crabzilla.stack.Snapshot;
import crabzilla.util.MultiMethod;
import lombok.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class CommandHandlerFn<A extends AggregateRoot> {

  protected final BiFunction<Event, A, A> stateTransitionFn;
  protected final Function<A, A> dependencyInjectionFn;
  private final MultiMethod mm ;

  protected CommandHandlerFn(@NonNull BiFunction<Event, A, A> stateTransitionFn,
                             @NonNull Function<A, A> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "handle");
  }

  public Optional<UnitOfWork> handle(final Command command, final Snapshot<A> snapshot) {

    try {
      return ((Optional<UnitOfWork>) mm.invoke(this, command, snapshot));
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return Optional.empty();

  }

}
