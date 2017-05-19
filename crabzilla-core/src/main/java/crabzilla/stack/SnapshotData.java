package crabzilla.stack;

import crabzilla.Version;
import crabzilla.model.Event;
import lombok.Value;

import java.util.List;

@Value
public class SnapshotData {

  Version version;
  List<Event> events;

}
