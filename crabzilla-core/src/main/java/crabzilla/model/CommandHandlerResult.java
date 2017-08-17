package crabzilla.model;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.function.Consumer;

@EqualsAndHashCode
public class CommandHandlerResult {

  private final EntityUnitOfWork unitOfWork;
  private final Throwable exception;

  private CommandHandlerResult(EntityUnitOfWork unitOfWork, Throwable exception) {
    this.unitOfWork = unitOfWork;
    this.exception = exception;
  }

  public void inCaseOfSuccess(@NonNull Consumer<EntityUnitOfWork> consumer) {
    if (unitOfWork != null) {
      consumer.accept(unitOfWork);
    }
  }

  public void inCaseOfError(@NonNull Consumer<Throwable> consumer) {
    if (exception != null) {
      consumer.accept(exception);
    }
  }

  static public CommandHandlerResult success(EntityUnitOfWork uow) {
    return new CommandHandlerResult(uow, null);
  }

  static public CommandHandlerResult error(@NonNull Throwable e) {
    return new CommandHandlerResult(null, e);
  }

}
