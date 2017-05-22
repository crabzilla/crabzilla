package crabzilla.example1.aggregates.customer;

import crabzilla.UnitOfWork;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.Event;
import crabzilla.stack.Snapshot;
import crabzilla.stack.StateTransitionsTracker;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.UnitOfWork.of;

public class CustomerCmdHandlerFn extends CommandHandlerFn<Customer> {

  @Inject
  public CustomerCmdHandlerFn(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                              final Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  public Optional<UnitOfWork> handle(CreateCustomerCmd cmd, Snapshot<Customer> snapshot) {
      val u = of(cmd, snapshot.nextVersion(), snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()));
      return Optional.of(u);
  }

  public Optional<UnitOfWork> handle(ActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    val u = of(cmd, snapshot.nextVersion(), snapshot.getInstance().activate(cmd.getReason()));
    return Optional.of(u);
  }

  public Optional<UnitOfWork> handle(DeactivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    val u = of(cmd, snapshot.nextVersion(), snapshot.getInstance().deactivate(cmd.getReason()));
    return Optional.of(u);
  }

  public Optional<UnitOfWork> handle(CreateActivateCustomerCmd cmd, Snapshot<Customer> snapshot) {
    val tracker = new StateTransitionsTracker<Customer>(snapshot.getInstance(), stateTransitionFn,
            dependencyInjectionFn);
    final List<Event> events = tracker
            .applyEvents(snapshot.getInstance().create(cmd.getTargetId(), cmd.getName()))
            .applyEvents(tracker.currentState().activate(cmd.getReason()))
            .collectEvents();
    val uow = of(cmd, snapshot.nextVersion(), events);
    return Optional.of(uow);
  }

}
