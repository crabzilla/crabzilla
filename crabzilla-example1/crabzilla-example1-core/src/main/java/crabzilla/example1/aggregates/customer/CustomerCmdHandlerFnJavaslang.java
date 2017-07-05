package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.model.UnitOfWork.unitOfWork;
import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

// TODO consider an example with some real business logic (a CreditService, for example)
@Slf4j
public class CustomerCmdHandlerFnJavaslang
        implements BiFunction<Command, Snapshot<Customer>, Either<Throwable, Optional<UnitOfWork>>> {

  private final BiFunction<Event, Customer, Customer> stateTransitionFn;
  private final Function<Customer, Customer> dependencyInjectionFn;

  @Inject
  public CustomerCmdHandlerFnJavaslang(final BiFunction<Event, Customer, Customer> stateTransitionFn,
                                       final Function<Customer, Customer> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
  }

  @Override
  public Either<Throwable, Optional<UnitOfWork>> apply(final Command cmd, final Snapshot<Customer> snapshot) {

    log.info("Will apply command {}", cmd);

    try {
      val uow = handle(cmd, snapshot);
      return Eithers.right(Optional.ofNullable(uow));
    } catch (Exception e) {
      return Eithers.left(e);
    }

  }

  private UnitOfWork handle(final Command cmd, final Snapshot<Customer> snapshot) {

    val targetInstance = snapshot.getInstance();
    val targetVersion = snapshot.getVersion();

    final UnitOfWork uow = Match(cmd).of(

      Case(instanceOf(CreateCustomerCmd.class), (command) ->

        unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.create(command.getTargetId(), command.getName()))
      ),

      Case(instanceOf(ActivateCustomerCmd.class), (command) ->

        unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

      Case(instanceOf(DeactivateCustomerCmd.class), (command) ->

        unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

      Case(instanceOf(CreateActivateCustomerCmd.class), (command) -> {

        val tracker = new StateTransitionsTracker<Customer>(targetInstance, stateTransitionFn,
                dependencyInjectionFn);
        val events = tracker
                .applyEvents(customer -> customer.create(command.getTargetId(), command.getName()))
                .applyEvents(customer -> customer.activate(command.getReason()))
                .collectEvents();

        return unitOfWork(cmd, targetVersion.nextVersion(), events);

      }),

      Case($(), o -> {

        log.warn("Can't apply command {}", cmd);
        return null;

      })

    );

    return uow;

  }

}
