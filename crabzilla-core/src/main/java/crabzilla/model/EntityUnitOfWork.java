package crabzilla.model;

import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class EntityUnitOfWork implements Serializable {

  @NonNull
  final UUID unitOfWorkId;
  @NonNull
  final EntityCommand command;
  @NonNull
  final Version version;
  @NonNull
  final List<DomainEvent> events;

  public static EntityUnitOfWork unitOfWork(EntityCommand command, Version version, List<DomainEvent> events) {
    return new EntityUnitOfWork(UUID.randomUUID(), command, version, events);
  }

  public EntityId targetId() {
    return command.getTargetId();
  }

}
