package crabzilla.example1.aggregates.customer;

import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.AggregateRootCmdHandler;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.stack.StateTransitionsTracker;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.UnitOfWork.of;
import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

public class CustomerCmdHandler extends AggregateRootCmdHandler<Customer> {

  @Inject
  public CustomerCmdHandler(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                            final Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  @Override
  public Optional<UnitOfWork> handle(final Command cmd, final Customer targetInstance, final Version targetVersion) {

    final UnitOfWork uow = Match(cmd).of(

      Case(instanceOf(CreateCustomerCmd.class), (command) ->

        of(cmd, targetVersion.nextVersion(),
                targetInstance.create(command.getTargetId(), command.getName()))
      ),

      Case(instanceOf(ActivateCustomerCmd.class), (command) ->

        of(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

      Case(instanceOf(DeactivateCustomerCmd.class), (command) ->

        of(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

      Case(instanceOf(CreateActivateCustomerCmd.class), (command) -> {

        val tracker = new StateTransitionsTracker<Customer>(targetInstance,
                stateTransitionFn, dependencyInjectionFn);
        final List<Event> events = tracker
                .applyEvents(targetInstance.create((CustomerId) cmd.getTargetId(), command.getName()))
                .applyEvents(tracker.currentState().activate(command.getReason()))
                .getEvents();
        return of(cmd, targetVersion.nextVersion(), events);
      }),

      Case($(), o -> null)

    );

    return uow == null ? Optional.empty() : Optional.of(uow);

  }
}
