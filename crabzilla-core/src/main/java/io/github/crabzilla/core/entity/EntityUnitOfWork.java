package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.DomainEvent;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class EntityUnitOfWork implements Serializable {

  private UUID unitOfWorkId;
  private EntityCommand command;
  private Version version;
  private List<DomainEvent> events;

  public static EntityUnitOfWork unitOfWork(@NonNull EntityCommand command,
                                            @NonNull Version version,
                                            @NonNull List<DomainEvent> events) {
    return new EntityUnitOfWork(UUID.randomUUID(), command, version, events);
  }

  public EntityId targetId() {
    return command.getTargetId();
  }

}
