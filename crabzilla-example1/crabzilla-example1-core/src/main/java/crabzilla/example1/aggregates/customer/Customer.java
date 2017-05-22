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

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements AggregateRoot {

  transient SampleService service;

  CustomerId id;
  String name;
  boolean isActive;
  String reason;

  protected List<Event> create(CustomerId id, String name) {

    validState(this.id == null, "customer already created");

    return asList(new CustomerCreated(id, name));
  }

  List<Event> activate(String reason) {

    return asList(new CustomerActivated(reason, service.now()));
  }

  List<Event> deactivate(String reason) {

    return asList(new CustomerDeactivated(reason, service.now()));
  }

  public static Customer of(CustomerId id, String name, boolean isActive, String reason) {

    return new Customer(null, id, name, isActive, reason);
  }

}
