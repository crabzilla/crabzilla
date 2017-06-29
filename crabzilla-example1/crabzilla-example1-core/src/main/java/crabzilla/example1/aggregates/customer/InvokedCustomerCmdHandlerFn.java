package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.model.StateTransitionsTracker;
import crabzilla.model.UnitOfWork;
import lombok.val;

import javax.inject.Inject;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.model.UnitOfWork.unitOfWork;

public class InvokedCustomerCmdHandlerFn {

  final BiFunction<Event, Customer, Customer> stateTransitionFn;
  final Function<Customer, Customer> dependencyInjectionFn;

  @Inject
  public InvokedCustomerCmdHandlerFn(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                                     final Function<Customer, Customer> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
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
    val tracker = new StateTransitionsTracker<Customer>(snapshot.getInstance(), stateTransitionFn, dependencyInjectionFn);
    val events = tracker
            .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
            .applyEvents(customer -> customer.activate(cmd.getReason()))
            .collectEvents();
    return unitOfWork(cmd, snapshot.nextVersion(), events);
  }

}
