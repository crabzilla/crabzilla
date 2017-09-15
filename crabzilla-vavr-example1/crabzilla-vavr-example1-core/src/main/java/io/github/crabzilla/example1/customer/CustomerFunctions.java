package io.github.crabzilla.example1.customer;

import io.github.crabzilla.core.Command;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.*;
import io.github.crabzilla.core.exceptions.UnknownCommandException;
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

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.Collections.emptyList;

public class CustomerFunctions {

  // tag::StateTransitionFn[]

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

  // end::StateTransitionFn[]

  // tag::CommandHandlerFn[]

  @Slf4j
  public static class CommandHandlerFn
          implements BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    @Override
    public EntityCommandResult apply(final EntityCommand cmd,
                                     final Snapshot<Customer> snapshot) {
      log.info("Will apply command {}", cmd);
      val tracker = trackerFactory.apply(snapshot);
      try {
        return EntityCommandResult.success(handle(cmd, tracker));
      } catch (Exception e) {
        return EntityCommandResult.error(e);
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

  // end::CommandHandlerFn[]

  // tag::CommandValidatorFn[]

  public static class CommandValidatorFn implements Function<EntityCommand, List<String>> {

    @Override
    public List<String> apply(EntityCommand command) {

      return Match(command).of(
        Case($(instanceOf(CreateCustomer.class)), (cmd) -> {
            val either = new CreateCustomerValidator().validate(cmd).toEither();
            return either.isRight() ? emptyList() : either.getLeft().asJava();
          }
        ),
        Case($(), o -> {
          return emptyList(); // it's always valid TODO
        }));
    }

  }

  // end::CommandValidatorFn[]

  static class CreateCustomerValidator {

    private static final String VALID_NAME_CHARS = "[a-zA-Z ]";

    Validation<Seq<String>, CustomerData.CreateCustomer> validate(CreateCustomer cmd) {
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

  static class CustomerEventListener implements Function<DomainEvent, List<Command>> {

    @Override
    public List<Command> apply(DomainEvent domainEvent) {
      return null;
    }
  }

}
