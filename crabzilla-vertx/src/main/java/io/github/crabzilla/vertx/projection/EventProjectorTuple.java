package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import lombok.Value;

@Value
class EventProjectorTuple {
  private String id;
  private DomainEvent event;
  EventProjectorTuple(String id, DomainEvent event) {
    this.id = id;
    this.event = event;
  }
}
