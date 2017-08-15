package crabzilla.example1.aggregates;

import crabzilla.model.EntityUnitOfWork;
import crabzilla.model.Snapshot;
import crabzilla.model.StateTransitionsTrackerFactory;
import crabzilla.stack.AbstractCommandsHandlerFn;
import crabzilla.stack.AbstractStateTransitionFn;
import lombok.val;

import javax.inject.Inject;

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

  public static class CommandHandlerFn extends AbstractCommandsHandlerFn<Customer> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    @Inject
    public CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    public EntityUnitOfWork handle(CustomerData.CreateCustomerCmd cmd, Snapshot<Customer> snapshot) {
        return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
    }

    public EntityUnitOfWork handle(CustomerData.ActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.DeactivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
    }

    public EntityUnitOfWork handle(CustomerData.CreateActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      val tracker = trackerFactory.create(snapshot.getInstance());
      val events = tracker
              .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
              .applyEvents(customer -> customer.activate(cmd.getReason()))
              .collectEvents();
      return unitOfWork(cmd, snapshot.nextVersion(), events);
    }

  }

}
