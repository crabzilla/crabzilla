package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.example1.services.SampleService;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Event;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements AggregateRoot {

  transient SampleService service;

  CustomerId id;
  String name;
  boolean isActive;
  String reason;

  List<Event> create(CustomerId id, String name) {

//    validState(false, "an error, just because I want to force it");
    validState(this.id == null, "customer already created");

    return singletonList(new CustomerCreated(id, name));
  }

  List<Event> activate(String reason) {

    return singletonList(new CustomerActivated(reason, service.now()));
  }

  List<Event> deactivate(String reason) {

    return singletonList(new CustomerDeactivated(reason, service.now()));
  }

  public static Customer of(CustomerId id, String name, boolean isActive, String reason) {

    return new Customer(null, id, name, isActive, reason);
  }

}
