package crabzilla.stack;

import crabzilla.Version;
import crabzilla.model.Event;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class SnapshotData implements Serializable {

  Version version;
  List<Event> events;

}
