package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.*;
import crabzilla.model.util.Either;
import crabzilla.model.util.Eithers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.model.UnitOfWork.of;
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
  public Either<Exception, Optional<UnitOfWork>> handle(final Command cmd, final Snapshot<Customer> snapshot) {

    log.info("Will handle command {}", cmd);

    try {
      val uow = _handle(cmd, snapshot);
      return Eithers.right(Optional.ofNullable(uow));
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

              val tracker = new StateTransitionsTracker<Customer>(targetInstance, stateTransitionFn,
                      dependencyInjectionFn);
              val events = tracker
                      .applyEvents(customer -> customer.create(command.getTargetId(), command.getName()))
                      .applyEvents(customer -> customer.activate(command.getReason()))
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
