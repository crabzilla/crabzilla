package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.Snapshot;
import crabzilla.model.StateTransitionsTrackerFactory;
import crabzilla.model.UnitOfWork;
import crabzilla.stack.AbstractCommandsHandlerFn;
import lombok.val;

import javax.inject.Inject;

import static crabzilla.model.UnitOfWork.unitOfWork;

public class CustomerCmdHandlerFn extends AbstractCommandsHandlerFn<Customer> {

  protected final StateTransitionsTrackerFactory<Customer> trackerFactory;

  @Inject
  public CustomerCmdHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
    this.trackerFactory = trackerFactory;
  }

  public UnitOfWork handle(CreateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
  }

  public UnitOfWork handle(ActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
  }

  public UnitOfWork handle(DeactivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    return unitOfWork(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
  }

  public UnitOfWork handle(CreateActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    val tracker = trackerFactory.create(snapshot.getInstance());
    val events = tracker
            .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
            .applyEvents(customer -> customer.activate(cmd.getReason()))
            .collectEvents();
    return unitOfWork(cmd, snapshot.nextVersion(), events);
  }

}
