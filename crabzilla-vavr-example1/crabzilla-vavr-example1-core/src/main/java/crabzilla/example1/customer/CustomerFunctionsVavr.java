package crabzilla.example1.customer;

import crabzilla.model.*;
import crabzilla.stack.UnknownCommandException;
import io.vavr.Function3;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.example1.customer.CustomerData.*;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.Collections.emptyList;

public class CustomerFunctionsVavr {

  public static class StateTransitionFn implements BiFunction<DomainEvent, Customer, Customer> {

    public Customer apply(final DomainEvent event, final Customer customer) {

      return Match(event).of(
        Case($(instanceOf(CustomerCreated.class)),
                (e) -> customer.withId(e.getId()).withName(e.getName())),
        Case($(instanceOf(CustomerActivated.class)),
                (e) -> customer.withReason(e.getReason()).withActive(true)),
        Case($(instanceOf(CustomerDeactivated.class)),
                (e) -> customer.withReason(e.getReason()).withActive(false)),
        Case($(), o -> customer));

    }
  }

  // TODO consider an example with some real business logic (a CreditService, for example)
  @Slf4j
  public static class CommandHandlerFn
          implements BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    public CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    @Override
    public CommandHandlerResult apply(final EntityCommand cmd, final Snapshot<Customer> snapshot) {
      CommandHandlerFn.log.info("Will apply command {}", cmd);
      final StateTransitionsTracker<Customer> tracker = trackerFactory.apply(snapshot);
      try {
        return CommandHandlerResult.success(handle(cmd, tracker));
      } catch (Exception e) {
        return CommandHandlerResult.error(e);
      }
    }

    private EntityUnitOfWork handle(final EntityCommand command,
                                    final StateTransitionsTracker<Customer> tracker) {

      final EntityUnitOfWork uow = Match(command).of(

        Case($(instanceOf(CreateCustomer.class)), (cmd) ->
          tracker.applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
                  .unitOfWorkFor(cmd)
        ),
        Case($(instanceOf(ActivateCustomer.class)), (cmd) ->
          tracker.applyEvents(customer -> customer.activate(cmd.getReason()))
                  .unitOfWorkFor(cmd)
        ),
        Case($(instanceOf(DeactivateCustomer.class)), (cmd) ->
          tracker.applyEvents(customer -> customer.deactivate(cmd.getReason()))
                  .unitOfWorkFor(cmd)
        ),
        Case($(instanceOf(CreateActivateCustomer.class)), (cmd) ->
          tracker
            .applyEvents(customer -> customer.create(cmd.getTargetId(), cmd.getName()))
            .applyEvents(customer -> customer.activate(cmd.getReason()))
            .unitOfWorkFor(cmd)
        ),
        Case($(), o -> {
          throw new UnknownCommandException("for command " + command.getClass().getSimpleName());
        })
      );

      return uow;

    }

  }

  public static class CommandValidatorFn implements Function<EntityCommand, List<String>> {

    @Override
    public List<String> apply(EntityCommand entityCommand) {
      return emptyList(); // it's always valid TODO
    }

    public java.util.List<String> validate(CreateCustomer cmd) {

        val either = new CreateCustomerValidator().validate(cmd).toEither();

        return either.isRight() ? emptyList() : either.getLeft().asJava();

      }

  }

  public static class CreateCustomerValidator {

    private static final String VALID_NAME_CHARS = "[a-zA-Z ]";

    public Validation<Seq<String>, CustomerData.CreateCustomer> validate(CreateCustomer cmd) {
      return Validation.combine(validateCmdId(cmd.getCommandId()),
              validateId(cmd.getTargetId()),
              validateName(cmd.getName())
      ).ap((Function3<UUID, CustomerId, String, CreateCustomer>) CreateCustomer::new);
    }

    private Validation<String, UUID>  validateCmdId(UUID commandId) {
      return commandId == null
              ? Validation.invalid("CommandId cannot be null ")
              : Validation.valid(commandId);
    }

    private Validation<String, CustomerData.CustomerId> validateId(CustomerId id) {
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

}
