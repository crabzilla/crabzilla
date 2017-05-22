package crabzilla.stack;

import crabzilla.model.Event;
import lombok.Value;

import java.util.List;

@Value
public class ProjectionData {

  String uowId;
  Long uowSequence;
  String targetId;
  List<Event> events;

}
