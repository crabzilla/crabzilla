package crabzilla.example1.aggregates.customer.events;

import crabzilla.model.Event;
import lombok.Value;

import java.time.Instant;

@Value
public class CustomerDeactivated implements Event {
  String reason;
  Instant when;
}
