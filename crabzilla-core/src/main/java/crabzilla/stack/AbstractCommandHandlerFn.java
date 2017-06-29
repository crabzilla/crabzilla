package crabzilla.stack;

import crabzilla.model.*;
import lombok.val;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class AbstractCommandHandlerFn<A extends AggregateRoot>
        implements BiFunction<Command, Snapshot<A>, Either<Throwable, Optional<UnitOfWork>>> {

  public static final String METHOD_NAME = "handle";

  public Either<Throwable, Optional<UnitOfWork>> apply(final Command command, final Snapshot<A> snapshot) {

    final MethodType methodType =
            MethodType.methodType(UnitOfWork.class, new Class<?>[] {command.getClass(), Snapshot.class});
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    try {
      final MethodHandle methodHandle = lookup.bind(this, METHOD_NAME, methodType);
      val uow = (UnitOfWork) methodHandle.invokeWithArguments(command, snapshot);
      return Eithers.right(Optional.ofNullable(uow)) ;
    } catch (IllegalAccessException | NoSuchMethodException e) {
      return Eithers.right(Optional.empty());
    } catch (Throwable throwable) {
      return Eithers.left(throwable);
    }

  }

}
