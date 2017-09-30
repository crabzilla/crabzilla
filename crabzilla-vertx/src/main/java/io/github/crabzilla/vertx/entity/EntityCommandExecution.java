package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.EntityUnitOfWork;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
public class EntityCommandExecution implements Serializable {

  public enum RESULT {
    FALLBACK,
    VALIDATION_ERROR,
    HANDLING_ERROR,
    CONCURRENCY_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS,
    ALREADY_PROCESSED // TODO
  }

  private RESULT result;
  private UUID commandId;
  private List<String> constraints;
  private Long uowSequence;
  private EntityUnitOfWork unitOfWork;

  public static EntityCommandExecution VALIDATION_ERROR(@NonNull List<String> constraints) {
    return new EntityCommandExecution(VALIDATION_ERROR, null, constraints, 0L, null);
  }

  public static EntityCommandExecution VALIDATION_ERROR(@NonNull UUID commandId, @NonNull List<String> constraints) {
    return new EntityCommandExecution(VALIDATION_ERROR, commandId, constraints, 0L, null);
  }

  public static EntityCommandExecution CONCURRENCY_ERROR(UUID commandId, String message) {
    return new EntityCommandExecution(CONCURRENCY_ERROR, commandId, singletonList(message), 0L, null);
  }

  public static EntityCommandExecution FALLBACK(@NonNull UUID commandId) {
    return new EntityCommandExecution(FALLBACK, commandId, emptyList(), 0L, null);
  }

  public static EntityCommandExecution HANDLING_ERROR(@NonNull UUID commandId, String message) {
    return new EntityCommandExecution(HANDLING_ERROR, commandId, singletonList(message), 0L, null);

  }

  public static EntityCommandExecution UNKNOWN_COMMAND(@NonNull UUID commandId) {
    return new EntityCommandExecution(UNKNOWN_COMMAND, commandId, emptyList(), 0L, null);
  }

  public static EntityCommandExecution SUCCESS(@NonNull EntityUnitOfWork uow, @NonNull Long uowSequence) {
    return new EntityCommandExecution(SUCCESS, uow.getCommand().getCommandId(), emptyList(), uowSequence, uow);
  }

  public static EntityCommandExecution ALREADY_PROCESSED(@NonNull EntityUnitOfWork uow, @NonNull Long uowSequence) {
    return new EntityCommandExecution(SUCCESS, uow.getCommand().getCommandId(), emptyList(), uowSequence, uow);
  }

}
