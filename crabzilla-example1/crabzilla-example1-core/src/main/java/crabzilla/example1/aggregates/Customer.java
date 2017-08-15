package crabzilla.example1.aggregates;

import crabzilla.example1.services.SampleInternalService;
import crabzilla.model.AggregateRoot;
import crabzilla.model.DomainEvent;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements AggregateRoot {

  transient SampleInternalService service;

  CustomerData.CustomerId id;
  String name;
  boolean isActive;
  String reason;

  List<DomainEvent> create(CustomerData.CustomerId id, String name) {

    validState(this.id == null, "customer already created");

    return singletonList(new CustomerData.CustomerCreated(id, name));
  }

  List<DomainEvent> activate(String reason) {

    return singletonList(new CustomerData.CustomerActivated(reason, service.now()));
  }

  List<DomainEvent> deactivate(String reason) {

    return singletonList(new CustomerData.CustomerDeactivated(reason, service.now()));
  }

  public static Customer of(CustomerData.CustomerId id, String name, boolean isActive, String reason) {

    return new Customer(null, id, name, isActive, reason);
  }

}
