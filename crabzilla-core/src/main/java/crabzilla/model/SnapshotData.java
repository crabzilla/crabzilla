package crabzilla.model;

import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class SnapshotData implements Serializable {

  Version version;
  List<DomainEvent> events;

}
