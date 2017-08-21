package io.github.crabzilla.stack;

import lombok.Value;
import io.github.crabzilla.model.DomainEvent;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class ProjectionData implements Serializable {

  UUID uowId;
  Long uowSequence;
  String targetId;
  List<DomainEvent> events;

}
