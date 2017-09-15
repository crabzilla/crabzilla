package io.github.crabzilla.example1.customer;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.Entity;
import io.github.crabzilla.example1.services.SampleInternalService;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.validState;

// tag::aggregate[]
@Value
@Wither
public class Customer implements Entity {

  private @NonNull transient SampleInternalService service;

  private CustomerId id;
  private String name;
  private boolean isActive;
  private String reason;

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
// end::aggregate[]
