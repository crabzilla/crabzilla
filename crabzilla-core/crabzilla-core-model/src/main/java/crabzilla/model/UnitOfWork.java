package crabzilla.model;

import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class UnitOfWork implements Serializable {

  @NonNull
  final UUID unitOfWorkId;
  @NonNull
  final Command command;
  @NonNull
  final Version version;
  @NonNull
  final List<Event> events;

  public static UnitOfWork of(Command command, Version version, List<Event> events) {
    return new UnitOfWork(UUID.randomUUID(), command, version, events);
  }


  public AggregateRootId targetId() {
    return command.getTargetId();
  }

}
