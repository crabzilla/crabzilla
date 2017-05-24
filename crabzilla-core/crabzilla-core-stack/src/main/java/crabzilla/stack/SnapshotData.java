package crabzilla.stack;

import crabzilla.model.Event;
import crabzilla.model.Version;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class SnapshotData implements Serializable {

  Version version;
  List<Event> events;

}
