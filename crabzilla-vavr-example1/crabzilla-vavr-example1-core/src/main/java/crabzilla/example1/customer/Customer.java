package crabzilla.example1.customer;

import crabzilla.example1.services.SampleInternalService;
import crabzilla.model.Aggregate;
import crabzilla.model.DomainEvent;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

import static crabzilla.example1.customer.CustomerData.*;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements Aggregate {

  transient SampleInternalService service;

  CustomerId id;
  String name;
  boolean isActive;
  String reason;

  List<DomainEvent> create(CustomerId id, String name) {
    validState(this.id == null, "customer already created");
    return singletonList(new CustomerCreated(id, name));
  }

  List<DomainEvent> activate(String reason) {
    validState(this.id != null, "unknown customer");
    return singletonList(new CustomerActivated(reason, service.now()));
  }

  List<DomainEvent> deactivate(String reason) {
    validState(this.id != null, "unknown customer");
    return singletonList(new CustomerDeactivated(reason, service.now()));
  }

}
