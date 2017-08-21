package io.github.crabzilla.example1.customer;

import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.model.Aggregate;
import io.github.crabzilla.model.DomainEvent;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements Aggregate {

  @NonNull transient SampleInternalService service;

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
