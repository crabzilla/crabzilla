package crabzilla.stack.vertx.verticles;

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

import static java.util.Collections.emptyList;

@Value
@Wither
public class CommandExecution implements Serializable {

  public enum RESULT {
    VALIDATION_ERROR,
    FALLBACK,
    BUSINESS_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS
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

  Optional<UUID> getCommandId() {
    return Optional.ofNullable(commandId);
  }

  Optional<List<String>> getConstraints() {
    return RESULT.VALIDATION_ERROR.equals(result) ? Optional.of(constraints) : Optional.empty();
  }

  Optional<Long> getUowSequence() {
    return RESULT.SUCCESS.equals(result) ? Optional.of(uowSequence) : Optional.empty();
  }

  Optional<UnitOfWork> getUnitOfWork() {
    return RESULT.SUCCESS.equals(result) ? Optional.of(unitOfWork) : Optional.empty();
  }

  static CommandExecution VALIDATION_ERROR(@NonNull List<String> constraints) {
    return new CommandExecution(RESULT.VALIDATION_ERROR, null, constraints, 0L,null);
  }

  static CommandExecution VALIDATION_ERROR(@NonNull UUID commandId, @NonNull List<String> constraints) {
    return new CommandExecution(RESULT.VALIDATION_ERROR, commandId, constraints, 0L,null);
  }

  static CommandExecution FALLBACK(@NonNull UUID commandId) {
    return new CommandExecution(RESULT.FALLBACK, commandId, emptyList(), 0L,null);
  }

  static CommandExecution BUSINESS_ERROR(@NonNull UUID commandId) {
    return new CommandExecution(RESULT.BUSINESS_ERROR, commandId, emptyList(), 0L,null);

  }

  static CommandExecution UNKNOWN_COMMAND(@NonNull UUID commandId) {
    return new CommandExecution(RESULT.UNKNOWN_COMMAND, commandId, emptyList(), 0L,null);
  }

  static CommandExecution SUCCESS(@NonNull UnitOfWork uow, @NonNull Long uowSequence) {
    return new CommandExecution(RESULT.SUCCESS, uow.getCommand().getCommandId(), emptyList(), uowSequence, uow);
  }
}
