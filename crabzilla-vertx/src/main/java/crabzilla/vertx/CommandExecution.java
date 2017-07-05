package crabzilla.vertx;

import crabzilla.model.UnitOfWork;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static crabzilla.vertx.CommandExecution.RESULT.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@Wither
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

  @Getter(AccessLevel.NONE)
  UUID commandId;
  @Getter(AccessLevel.NONE)
  List<String> constraints;
  @Getter(AccessLevel.NONE)
  Long uowSequence;
  @Getter(AccessLevel.NONE)
  UnitOfWork unitOfWork;

  public Optional<UUID> getCommandId() {
    return Optional.ofNullable(commandId);
  }

  public Optional<List<String>> getConstraints() {
    return VALIDATION_ERROR.equals(result) || CONCURRENCY_ERROR.equals(result) ?
            Optional.of(constraints) : Optional.empty();
  }

  public Optional<Long> getUowSequence() {
    return SUCCESS.equals(result) ? Optional.of(uowSequence) : Optional.empty();
  }

  public Optional<UnitOfWork> getUnitOfWork() {
    return SUCCESS.equals(result) ? Optional.of(unitOfWork) : Optional.empty();
  }

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

  public static CommandExecution HANDLING_ERROR(@NonNull UUID commandId) {
    return new CommandExecution(HANDLING_ERROR, commandId, emptyList(), 0L, null);

  }

  public static CommandExecution UNKNOWN_COMMAND(@NonNull UUID commandId) {
    return new CommandExecution(UNKNOWN_COMMAND, commandId, emptyList(), 0L, null);
  }

  public static CommandExecution SUCCESS(@NonNull UnitOfWork uow, @NonNull Long uowSequence) {
    return new CommandExecution(SUCCESS, uow.getCommand().getCommandId(), emptyList(), uowSequence, uow);
  }
}
