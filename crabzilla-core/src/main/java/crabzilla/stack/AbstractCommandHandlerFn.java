package crabzilla.stack;

import crabzilla.model.*;
import crabzilla.model.util.Either;
import crabzilla.model.util.Eithers;
import crabzilla.model.util.MultiMethod;
import lombok.NonNull;
import lombok.val;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractCommandHandlerFn<A extends AggregateRoot>
        implements BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> {

  protected final BiFunction<Event, A, A> stateTransitionFn;
  protected final Function<A, A> dependencyInjectionFn;
  private final MultiMethod mm ;

  protected AbstractCommandHandlerFn(@NonNull BiFunction<Event, A, A> stateTransitionFn,
                                     @NonNull Function<A, A> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "handle");
  }

  public Either<Exception, Optional<UnitOfWork>> apply(final Command command, final Snapshot<A> snapshot) {

    try {
      val unitOfWork = ((UnitOfWork) mm.invoke(this, command, snapshot));
      return Eithers.right(Optional.ofNullable(unitOfWork));
    } catch (IllegalAccessException | NoSuchMethodException e) {
      return Eithers.right(Optional.empty());
    } catch (Exception e) {
      return Eithers.left(e);
    }

  }

}
