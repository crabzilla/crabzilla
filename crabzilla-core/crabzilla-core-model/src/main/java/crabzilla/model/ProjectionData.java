package crabzilla.model;

import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class ProjectionData implements Serializable {

  String uowId;
  Long uowSequence;
  String targetId;
  List<Event> events;

}
