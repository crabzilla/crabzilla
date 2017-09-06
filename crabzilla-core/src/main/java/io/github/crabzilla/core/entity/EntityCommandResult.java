package io.github.crabzilla.core.entity;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.function.Consumer;

@EqualsAndHashCode
public class EntityCommandResult {

  private final EntityUnitOfWork unitOfWork;
  private final Throwable exception;

  private EntityCommandResult(EntityUnitOfWork unitOfWork, Throwable exception) {
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

  static public EntityCommandResult success(@NonNull EntityUnitOfWork uow) {
    return new EntityCommandResult(uow, null);
  }

  static public EntityCommandResult error(@NonNull Throwable e) {
    return new EntityCommandResult(null, e);
  }

}
