package io.github.crabzilla.model;

import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class EntityUnitOfWork implements Serializable {

  UUID unitOfWorkId;
  EntityCommand command;
  Version version;
  List<DomainEvent> events;

  public static EntityUnitOfWork unitOfWork(@NonNull EntityCommand command,
                                            @NonNull Version version,
                                            @NonNull List<DomainEvent> events) {
    return new EntityUnitOfWork(UUID.randomUUID(), command, version, events);
  }

  public EntityId targetId() {
    return command.getTargetId();
  }

}
