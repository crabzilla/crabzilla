package crabzilla.example1.aggregates;

import crabzilla.model.*;
import crabzilla.stack.AbstractCommandValidatorFn;
import javaslang.Function3;
import javaslang.collection.CharSeq;
import javaslang.collection.List;
import javaslang.control.Validation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static crabzilla.model.EntityUnitOfWork.unitOfWork;
import static java.util.Collections.emptyList;
import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

public class CustomerFunctionsVavr {
  
  public static class CustomerSupplierFn implements Supplier<Customer> {
    final Customer customer = new Customer(null, null,  null, false, null);
    @Override
    public Customer get() {
      return customer;
    }
  }

  public static class CustomerStateTransitionFn implements BiFunction<DomainEvent, Customer, Customer> {

    public Customer apply(final DomainEvent event, final Customer instance) {

      return Match(event).of(

              Case(instanceOf(CustomerData.CustomerCreated.class),
                      (e) -> instance.withId(e.getId()).withName(e.getName())),

              Case(instanceOf(CustomerData.CustomerActivated.class),
                      (e) -> instance.withReason(e.getReason()).withActive(true)),

              Case(instanceOf(CustomerData.CustomerDeactivated.class),
                      (e) -> instance.withReason(e.getReason()).withActive(false))

      );
    }
  }

  // TODO consider an example with some real business logic (a CreditService, for example)
  @Slf4j
  public static class CustomerCmdHandlerFnJavaslang
          implements BiFunction<EntityCommand, Snapshot<Customer>, Either<Throwable, Optional<EntityUnitOfWork>>> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    @Inject
    public CustomerCmdHandlerFnJavaslang(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    @Override
    public Either<Throwable, Optional<EntityUnitOfWork>> apply(final EntityCommand cmd, final Snapshot<Customer> snapshot) {

      CustomerCmdHandlerFnJavaslang.log.info("Will apply command {}", cmd);

      try {
        val uow = handle(cmd, snapshot);
        return Eithers.right(Optional.ofNullable(uow));
      } catch (Exception e) {
        return Eithers.left(e);
      }

    }

    private EntityUnitOfWork handle(final EntityCommand cmd, final Snapshot<Customer> snapshot) {

      val targetInstance = snapshot.getInstance();
      val targetVersion = snapshot.getVersion();

      final EntityUnitOfWork uow = Match(cmd).of(

              Case(instanceOf(CustomerData.CreateCustomerCmd.class), (command) ->

                      unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.create(command.getTargetId(), command.getName()))
              ),

              Case(instanceOf(CustomerData.ActivateCustomerCmd.class), (command) ->

                      unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

              Case(instanceOf(CustomerData.DeactivateCustomerCmd.class), (command) ->

                      unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

              Case(instanceOf(CustomerData.CreateActivateCustomerCmd.class), (command) -> {

                val tracker = trackerFactory.create(targetInstance);
                val events = tracker
                        .applyEvents(customer -> customer.create(command.getTargetId(), command.getName()))
                        .applyEvents(customer -> customer.activate(command.getReason()))
                        .collectEvents();

                return unitOfWork(cmd, targetVersion.nextVersion(), events);

              }),

              Case($(), o -> {

                CustomerCmdHandlerFnJavaslang.log.warn("Can't apply command {}", cmd);
                return null;

              })

      );

      return uow;

    }

  }

  public static class CreateCustomerCmdValidator {

      private static final String VALID_NAME_CHARS = "[a-zA-Z ]";

      public Validation<List<String>, CustomerData.CreateCustomerCmd> validate(CustomerData.CreateCustomerCmd cmd) {
        return Validation.combine(validateCmdId(cmd.getCommandId()),
                                  validateId(cmd.getTargetId()),
                                  validateName(cmd.getName())
        ).ap((Function3<UUID, CustomerData.CustomerId, String, CustomerData.CreateCustomerCmd>) CustomerData.CreateCustomerCmd::new);
      }

    private Validation<String, UUID>  validateCmdId(UUID commandId) {
      return commandId == null
              ? Validation.invalid("CommandId cannot be null ")
              : Validation.valid(commandId);
    }

    private Validation<String, CustomerData.CustomerId> validateId(CustomerData.CustomerId id) {
      return id == null
              ? Validation.invalid("CustomerId cannot be null ")
              : Validation.valid(id);
    }

    private Validation<String, String> validateName(String name) {
        return CharSeq.of(name).replaceAll(VALID_NAME_CHARS, "").transform(seq -> seq.isEmpty()
                ? Validation.valid(name)
                : Validation.invalid("Name contains invalid characters: '"
                + seq.distinct().sorted() + "'"));
      }

    }

  public static class CustomerCommandValidatorFn extends AbstractCommandValidatorFn {

      public java.util.List<String> validate(CustomerData.CreateCustomerCmd cmd) {

        val either = new CreateCustomerCmdValidator().validate(cmd).toEither();

        return either.isRight() ? emptyList() : either.getLeft().toJavaList();

      }

    public java.util.List<String> validate(CustomerData.ActivateCustomerCmd cmd) {

      return emptyList(); // it's always valid

    }
  }

}
