package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class ProjectionData implements Serializable {

  private UUID uowId;
  private Long uowSequence;
  private String targetId;
  private List<DomainEvent> events;

}
