package crabzilla.example1.util;

import crabzilla.model.*;
import lombok.val;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiFunction;

public abstract class AbstractCommandsHandlerFn<A extends Aggregate>
        implements BiFunction<EntityCommand, Snapshot<A>, CommandHandlerResult> {

  static final String METHOD_NAME = "handle";
  final MethodHandles.Lookup lookup = MethodHandles.lookup();

  public CommandHandlerResult apply(final EntityCommand command, final Snapshot<A> snapshot) {

    final MethodType methodType =
            MethodType.methodType(EntityUnitOfWork.class, new Class<?>[] {command.getClass(), Snapshot.class});


    try {
      final MethodHandle methodHandle = lookup.bind(this, METHOD_NAME, methodType);
      val uow = (EntityUnitOfWork) methodHandle.invokeWithArguments(command, snapshot);
      return CommandHandlerResult.success(uow) ;
    } catch (IllegalAccessException | NoSuchMethodException e) {
      return CommandHandlerResult.success(null);
    } catch (Throwable throwable) {
      return CommandHandlerResult.error(throwable);
    }

  }

}
