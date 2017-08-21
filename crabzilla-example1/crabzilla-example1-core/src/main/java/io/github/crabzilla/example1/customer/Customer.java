package io.github.crabzilla.example1.customer;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.model.Aggregate;
import io.github.crabzilla.model.DomainEvent;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements Aggregate {

  @NonNull
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
    validState(this.id != null, "unknown customer");
    return singletonList(new CustomerData.CustomerActivated(reason, service.now()));
  }

  List<DomainEvent> deactivate(String reason) {
    validState(this.id != null, "unknown customer");
    return singletonList(new CustomerData.CustomerDeactivated(reason, service.now()));
  }

}
