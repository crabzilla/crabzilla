package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.*;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.model.UnitOfWork.of;

public class CustomerCmdHandlerFn extends CommandHandlerFn<Customer> {

  @Inject
  public CustomerCmdHandlerFn(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                              final Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  public UnitOfWork handle(CreateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      return of(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
  }

  public UnitOfWork handle(ActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    return of(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
  }

  public UnitOfWork handle(DeactivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    return of(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
  }

  public UnitOfWork handle(CreateActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    val tracker = new StateTransitionsTracker<Customer>(snapshot.getInstance(), stateTransitionFn,
            dependencyInjectionFn);
    final List<Event> events = tracker
            .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
            .applyEvents(customer -> customer.activate(cmd.getReason()))
            .collectEvents();
    return of(cmd, snapshot.nextVersion(), events);
  }

}
