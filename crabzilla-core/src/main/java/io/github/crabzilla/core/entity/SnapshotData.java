package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.DomainEvent;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class SnapshotData implements Serializable {

  private Version version;
  private List<DomainEvent> events;

}
