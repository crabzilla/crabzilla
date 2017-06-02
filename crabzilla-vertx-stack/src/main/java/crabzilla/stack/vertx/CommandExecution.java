package crabzilla.stack.vertx;

import crabzilla.model.UnitOfWork;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

  UUID commandId;
  RESULT result;

  Long uowSequence;
  @Getter(AccessLevel.NONE)
  List<String> constraints;
  @Getter(AccessLevel.NONE)
  UnitOfWork unitOfWork;

  public Optional<Long> getUowSequence() {
    return RESULT.SUCCESS.equals(result) ? Optional.of(uowSequence) : Optional.empty();
  }

  public Optional<List<String>> getConstraints() {
    return RESULT.VALIDATION_ERROR.equals(result) ? Optional.of(constraints) : Optional.empty();
  }

  public Optional<UnitOfWork> getUnitOfWork() {
    return RESULT.SUCCESS.equals(result) ? Optional.of(unitOfWork) : Optional.empty();
  }

  public static CommandExecution VALIDATION_ERROR(UUID commandId, List<String> constraints) {
    return new CommandExecution(commandId, RESULT.VALIDATION_ERROR, 0L, constraints, null);
  }
  public static CommandExecution FALLBACK(UUID commandId) {
    return new CommandExecution(commandId, RESULT.FALLBACK, 0L, Collections.emptyList(), null);

  }  public static CommandExecution BUSINESS_ERROR(UUID commandId) {
    return new CommandExecution(commandId, RESULT.BUSINESS_ERROR, 0L, Collections.emptyList(), null);

  }  public static CommandExecution UNKNOWN_COMMAND(UUID commandId) {
    return new CommandExecution(commandId, RESULT.UNKNOWN_COMMAND, 0L, Collections.emptyList(), null);
  }
  public static CommandExecution SUCCESS(UnitOfWork uow, Long uowSequence) {
    return new CommandExecution(uow.getCommand().getCommandId(), RESULT.SUCCESS, uowSequence,
            Collections.emptyList(), uow);
  }
}
