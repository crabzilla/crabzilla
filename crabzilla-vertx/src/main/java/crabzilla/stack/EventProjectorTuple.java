package crabzilla.stack;

import crabzilla.model.DomainEvent;

public class EventProjectorTuple {
  public final String id;
  public final DomainEvent event;
  public EventProjectorTuple(String id, DomainEvent event) {
    this.id = id;
    this.event = event;
  }
}
