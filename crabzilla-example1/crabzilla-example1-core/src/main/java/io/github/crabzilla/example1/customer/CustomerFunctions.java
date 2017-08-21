package io.github.crabzilla.example1.customer;

import io.github.crabzilla.example1.util.AbstractCommandsHandlerFn;
import io.github.crabzilla.example1.util.AbstractStateTransitionFn;
import io.github.crabzilla.model.EntityCommand;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.model.Snapshot;
import io.github.crabzilla.model.StateTransitionsTrackerFactory;
import lombok.val;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.github.crabzilla.model.EntityUnitOfWork.unitOfWork;

public class CustomerFunctions {

  // tag::StateTransitionFn[]

  public static class StateTransitionFn extends AbstractStateTransitionFn<Customer> {

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

  // end::StateTransitionFn[]

  // tag::CommandValidatorFn[]

  public static class CommandValidatorFn implements Function<EntityCommand, List<String>> {

    @Override
    public List<String> apply(EntityCommand entityCommand) {
      return Collections.emptyList(); // all commands are valid
    }

  }

  // end::CommandValidatorFn[]

  // tag::CommandHandlerFn[]

  public static class CommandHandlerFn extends AbstractCommandsHandlerFn<Customer> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    @Inject
    public CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    public EntityUnitOfWork handle(CreateCustomer cmd, Snapshot<Customer> snapshot) {
        return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance()
                .create(cmd.getTargetId(), cmd.getName()));
    }

    public EntityUnitOfWork handle(ActivateCustomer cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(),
              snapshot.getInstance().activate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(DeactivateCustomer cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(),
              snapshot.getInstance().deactivate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CreateActivateCustomer cmd, Snapshot<Customer> snapshot) {
      val tracker = trackerFactory.apply(snapshot);
      val events = tracker
              .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
              .applyEvents(customer -> customer.activate(cmd.getReason()))
              .collectEvents();
      return unitOfWork(cmd, snapshot.nextVersion(), events);
    }

  }

  // end::CommandHandlerFn[]

}
