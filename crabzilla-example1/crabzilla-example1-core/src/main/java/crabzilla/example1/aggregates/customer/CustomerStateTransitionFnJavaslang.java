package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.model.Event;

import java.util.function.BiFunction;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

public class CustomerStateTransitionFnJavaslang implements BiFunction<Event, Customer, Customer> {

  public Customer apply(final Event event, final Customer instance) {

    return Match(event).of(

      Case(instanceOf(CustomerCreated.class),
              (e) -> instance.withId(e.getId()).withName(e.getName())),

      Case(instanceOf(CustomerActivated.class),
              (e) -> instance.withReason(e.getReason()).withActive(true)),

      Case(instanceOf(CustomerDeactivated.class),
              (e) -> instance.withReason(e.getReason()).withActive(false))

    );
  }
}
