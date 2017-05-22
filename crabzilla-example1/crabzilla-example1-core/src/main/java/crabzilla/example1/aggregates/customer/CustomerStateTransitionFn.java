package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.model.StateTransitionFn;

public class CustomerStateTransitionFn extends StateTransitionFn<Customer> {

  public Customer apply(final CustomerCreated event, final Customer instance) {
    return instance.withId(event.getId()).withName(event.getName());
  }

  public Customer apply(final CustomerActivated event, final Customer instance) {
    return instance.withActive(true).withReason(event.getReason());
  }

  public Customer apply(final CustomerDeactivated event, final Customer instance) {
    return instance.withActive(false).withReason(event.getReason());
  }

}
