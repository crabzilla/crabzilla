package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.model.AbstractStateTransitionFn;

public class CustomerStateTransitionFn extends AbstractStateTransitionFn<Customer> {

  public Customer on(final CustomerCreated event, final Customer instance) {
    return instance.withId(event.getId()).withName(event.getName());
  }

  public Customer on(final CustomerActivated event, final Customer instance) {
    return instance.withActive(true).withReason(event.getReason());
  }

  public Customer on(final CustomerDeactivated event, final Customer instance) {
    return instance.withActive(false).withReason(event.getReason());
  }

}
