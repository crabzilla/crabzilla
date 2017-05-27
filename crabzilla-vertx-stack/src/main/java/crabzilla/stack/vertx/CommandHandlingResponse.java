package crabzilla.stack.vertx;

import crabzilla.model.UnitOfWork;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Value
@Wither
public class CommandHandlingResponse {

  public enum RESULT {
    VALIDATION_ERROR,
    FALLBACK,
    BUSINESS_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS,
    SUCCESS_PROJECTION_FALLBACK
  }

  UUID commandId;
  RESULT result;

  @Getter(AccessLevel.NONE)
  List<String> constraints;
  @Getter(AccessLevel.NONE)
  UnitOfWork unitOfWork;

  public Optional<List<String>> getConstraints() {
    return RESULT.VALIDATION_ERROR.equals(result) ? Optional.of(constraints) : Optional.empty();
  }

  public Optional<UnitOfWork> getUnitOfWork() {
    return RESULT.SUCCESS.equals(result) ? Optional.of(unitOfWork) : Optional.empty();
  }

  public static CommandHandlingResponse VALIDATION_ERROR(UUID commandId, List<String> constraints) {
    return new CommandHandlingResponse(commandId, RESULT.VALIDATION_ERROR, constraints, null);
  }
  public static CommandHandlingResponse FALLBACK(UUID commandId) {
    return new CommandHandlingResponse(commandId, RESULT.FALLBACK, Collections.emptyList(), null);

  }  public static CommandHandlingResponse BUSINESS_ERROR(UUID commandId) {
    return new CommandHandlingResponse(commandId, RESULT.BUSINESS_ERROR, Collections.emptyList(), null);

  }  public static CommandHandlingResponse UNKNOWN_COMMAND(UUID commandId) {
    return new CommandHandlingResponse(commandId, RESULT.UNKNOWN_COMMAND, Collections.emptyList(), null);
  }
  public static CommandHandlingResponse SUCCESS(UnitOfWork uow) {
    return new CommandHandlingResponse(uow.getCommand().getCommandId(), RESULT.SUCCESS, Collections.emptyList(), uow);
  }
  public static CommandHandlingResponse SUCCESS_PROJECTION_FALLBACK(UnitOfWork uow) {
    return new CommandHandlingResponse(uow.getCommand().getCommandId(), RESULT.SUCCESS_PROJECTION_FALLBACK, Collections.emptyList(), uow);
  }
}
