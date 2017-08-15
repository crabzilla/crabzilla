package crabzilla.model;

import lombok.EqualsAndHashCode;

import java.util.function.Consumer;

@EqualsAndHashCode
public class CommandHandlerResult {

  private final EntityUnitOfWork unitOfWork;
  private final Exception exception;

  private CommandHandlerResult(EntityUnitOfWork unitOfWork, Exception exception) {
    this.unitOfWork = unitOfWork;
    this.exception = exception;
  }

  public void inCaseOfSuccess(Consumer<EntityUnitOfWork> consumer) {
    if (unitOfWork != null) {
      consumer.accept(unitOfWork);
    }
  }

  public void inCaseOfError(Consumer<Exception> consumer) {
    if (exception != null) {
      consumer.accept(exception);
    }
  }

  static public CommandHandlerResult success(EntityUnitOfWork uow) {
    return new CommandHandlerResult(uow, null);
  }

  static public CommandHandlerResult error(Exception e) {
    return new CommandHandlerResult(null, e);
  }

}
