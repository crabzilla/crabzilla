package crabzilla.example1.aggregates.customer.events;

import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.model.Event;
import lombok.Value;

@Value
public class CustomerCreated implements Event {
  CustomerId id;
  String name;
}
