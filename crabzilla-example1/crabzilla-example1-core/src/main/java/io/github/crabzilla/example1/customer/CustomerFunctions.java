package io.github.crabzilla.example1.customer;

import lombok.val;
import io.github.crabzilla.example1.util.AbstractCommandsHandlerFn;
import io.github.crabzilla.example1.util.AbstractStateTransitionFn;
import io.github.crabzilla.model.EntityCommand;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.model.Snapshot;
import io.github.crabzilla.model.StateTransitionsTrackerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CustomerFunctions {

  public static class StateTransitionFn extends AbstractStateTransitionFn<Customer> {

    public Customer on(final CustomerData.CustomerCreated event, final Customer instance) {
      return instance.withId(event.getId()).withName(event.getName());
    }

    public Customer on(final CustomerData.CustomerActivated event, final Customer instance) {
      return instance.withActive(true).withReason(event.getReason());
    }

    public Customer on(final CustomerData.CustomerDeactivated event, final Customer instance) {
      return instance.withActive(false).withReason(event.getReason());
    }

  }

  public static class CommandValidatorFn implements Function<EntityCommand, List<String>> {

    @Override
    public List<String> apply(EntityCommand entityCommand) {
      return Collections.emptyList(); // all commands are valid
    }

  }

  public static class CommandHandlerFn extends AbstractCommandsHandlerFn<Customer> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    @Inject
    public CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    public EntityUnitOfWork handle(CustomerData.CreateCustomer cmd, Snapshot<Customer> snapshot) {
        return EntityUnitOfWork.unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
    }

    public EntityUnitOfWork handle(CustomerData.ActivateCustomer cmd, Snapshot<Customer> snapshot) {
      return EntityUnitOfWork.unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.DeactivateCustomer cmd, Snapshot<Customer> snapshot) {
      return EntityUnitOfWork.unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.CreateActivateCustomer cmd, Snapshot<Customer> snapshot) {
      val tracker = trackerFactory.apply(snapshot);
      val events = tracker
              .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
              .applyEvents(customer -> customer.activate(cmd.getReason()))
              .collectEvents();
      return EntityUnitOfWork.unitOfWork(cmd, snapshot.nextVersion(), events);
    }

  }

}
