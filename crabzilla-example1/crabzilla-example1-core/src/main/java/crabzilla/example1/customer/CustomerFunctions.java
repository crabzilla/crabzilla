package crabzilla.example1.customer;

import crabzilla.example1.util.AbstractCommandsHandlerFn;
import crabzilla.example1.util.AbstractStateTransitionFn;
import crabzilla.model.EntityCommand;
import crabzilla.model.EntityUnitOfWork;
import crabzilla.model.Snapshot;
import crabzilla.model.StateTransitionsTrackerFactory;
import lombok.val;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.model.EntityUnitOfWork.unitOfWork;

public class CustomerFunctions {

  public static class SupplierFn implements Supplier<Customer> {
    final Customer customer = new Customer(null, null,  null, false, null);
    @Override
    public Customer get() {
      return customer;
    }
  }

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
        return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
    }

    public EntityUnitOfWork handle(CustomerData.ActivateCustomer cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.DeactivateCustomer cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.CreateActivateCustomer cmd, Snapshot<Customer> snapshot) {
      val tracker = trackerFactory.apply(snapshot.getInstance());
      val events = tracker
              .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
              .applyEvents(customer -> customer.activate(cmd.getReason()))
              .collectEvents();
      return unitOfWork(cmd, snapshot.nextVersion(), events);
    }

  }

}
