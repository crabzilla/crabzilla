package crabzilla.example1.aggregates.customer;

import crabzilla.UnitOfWork;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.Command;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.Event;
import crabzilla.stack.Snapshot;
import crabzilla.stack.StateTransitionsTracker;
import crabzilla.util.Either;
import crabzilla.util.Eithers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.UnitOfWork.of;
import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

@Slf4j
public class CustomerCmdHandlerFnJavaslang extends CommandHandlerFn<Customer> {

  @Inject
  public CustomerCmdHandlerFnJavaslang(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                                       final Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  @Override
  public Either<Exception, UnitOfWork> handle(final Command cmd, final Snapshot<Customer> snapshot) {

    log.info("Will handle command {}", cmd);

    try {
      val uow = _handle(cmd, snapshot);
      return Eithers.right(uow);
    } catch (Exception e) {
      return Eithers.left(e);
    }

  }

  private UnitOfWork _handle(final Command cmd, final Snapshot<Customer> snapshot) {

    val targetInstance = snapshot.getInstance();
    val targetVersion = snapshot.getVersion();

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
                      .collectEvents();
              return of(cmd, targetVersion.nextVersion(), events);
            }),

            Case($(), o -> {

              log.warn("Can't handle command {}", cmd);
              return null;
            })

    );

    return uow;

  }

}
