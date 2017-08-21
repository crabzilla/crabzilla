package io.github.crabzilla.stack;

import io.github.crabzilla.model.EntityUnitOfWork;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static io.github.crabzilla.stack.CommandExecution.RESULT.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
public class CommandExecution implements Serializable {

  public enum RESULT {
    FALLBACK,
    VALIDATION_ERROR,
    HANDLING_ERROR,
    CONCURRENCY_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS,
    COMMAND_ALREADY_PROCESSED // TODO
  }

  RESULT result;
  UUID commandId;
  List<String> constraints;
  Long uowSequence;
  EntityUnitOfWork unitOfWork;

  public static CommandExecution VALIDATION_ERROR(@NonNull List<String> constraints) {
    return new CommandExecution(VALIDATION_ERROR, null, constraints, 0L, null);
  }

  public static CommandExecution VALIDATION_ERROR(@NonNull UUID commandId, @NonNull List<String> constraints) {
    return new CommandExecution(VALIDATION_ERROR, commandId, constraints, 0L, null);
  }

  public static CommandExecution CONCURRENCY_ERROR(UUID commandId, String message) {
    return new CommandExecution(CONCURRENCY_ERROR, commandId, singletonList(message), 0L, null);
  }

  public static CommandExecution FALLBACK(@NonNull UUID commandId) {
    return new CommandExecution(FALLBACK, commandId, emptyList(), 0L, null);
  }

  public static CommandExecution HANDLING_ERROR(@NonNull UUID commandId, String message) {
    return new CommandExecution(HANDLING_ERROR, commandId, singletonList(message), 0L, null);

  }

  public static CommandExecution UNKNOWN_COMMAND(@NonNull UUID commandId) {
    return new CommandExecution(UNKNOWN_COMMAND, commandId, emptyList(), 0L, null);
  }

  public static CommandExecution SUCCESS(@NonNull EntityUnitOfWork uow, @NonNull Long uowSequence) {
    return new CommandExecution(SUCCESS, uow.getCommand().getCommandId(), emptyList(), uowSequence, uow);
  }

}
