package crabzilla.stack.vertx.verticles;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Command;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.model.util.Either;
import crabzilla.model.util.Eithers;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotMessage;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.Value;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static crabzilla.stack.vertx.verticles.CommandExecution.RESULT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class CommandHandlerVerticleTest {

  Vertx vertx;
  Cache<String, Snapshot<Customer>> cache;
  CircuitBreaker circuitBreaker;

  @Mock
  Function<Command, List<String>> validatorFn;
  @Mock
  Function<String, SnapshotMessage<Customer>> snapshotReaderFn;
  @Mock
  BiFunction<Command, Snapshot<Customer>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn;
  @Mock
  EventRepository eventRepository;

  @Before
  public void setUp(TestContext context) {

    MockitoAnnotations.initMocks(this);

    vertx = new VertxFactory().vertx();
    cache = Caffeine.newBuilder().build();
    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    val verticle = new CommandHandlerVerticle<Customer>(Customer.class, snapshotReaderFn, cmdHandlerFn,
                              validatorFn, eventRepository, cache, vertx, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void valid_command_get_SUCCESS(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer");
    val expectedSnapshot = new Snapshot<Customer>(new CustomerSupplierFn().get(), new Version(0));
    val expectedMessage = new SnapshotMessage<Customer>(expectedSnapshot, SnapshotMessage.LoadedFromEnum.FROM_DB);
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = UnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());
    when(snapshotReaderFn.apply(eq(customerId.getStringValue()))).thenReturn(expectedMessage);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(expectedSnapshot)))
            .thenReturn(Eithers.right(Optional.of(expectedUow)));
    when(eventRepository.append(any(UnitOfWork.class))).thenReturn(1L);

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).apply(eq(createCustomerCmd));
      verify(snapshotReaderFn).apply(eq(createCustomerCmd.getTargetId().getStringValue()));
      verify(cmdHandlerFn).apply(createCustomerCmd, expectedSnapshot);

      ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);
      verify(eventRepository).append(argument.capture());

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.SUCCESS, response.getResult());
      tc.assertEquals(1L, response.getUowSequence().get());

      val resultUnitOfWork = response.getUnitOfWork().get();

      tc.assertEquals(expectedUow.getCommand(), resultUnitOfWork.getCommand());
      tc.assertEquals(expectedUow.getEvents(), resultUnitOfWork.getEvents());
      tc.assertEquals(expectedUow.getVersion(), resultUnitOfWork.getVersion());

      async.complete();

    });

  }

  @Test
  public void an_invalid_command_get_VALIDATION_ERROR(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");
    val expectedSnapshot = new Snapshot<Customer>(new CustomerSupplierFn().get(), new Version(0));
    val expectedMessage = new SnapshotMessage<Customer>(expectedSnapshot, SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(singletonList("An error"));

    when(snapshotReaderFn.apply(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).apply(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.VALIDATION_ERROR, response.getResult());

      tc.assertEquals(asList("An error"), response.getConstraints().get());

      async.complete();

    });

  }

  @Value
  class UnknownCommand implements Command {
    UUID commandId;
    CustomerId targetId;
  }

  @Test
  public void an_unknown_command_get_UNKNOWN_COMMAND(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new UnknownCommand(UUID.randomUUID(), customerId);
    val expectedSnapshot = new Snapshot<Customer>(new CustomerSupplierFn().get(), new Version(0));
    val expectedMessage = new SnapshotMessage<Customer>(expectedSnapshot, SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());
    when(snapshotReaderFn.apply(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(expectedSnapshot)))
            .thenReturn(Eithers.right(Optional.empty()));

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).apply(eq(createCustomerCmd));
      verify(snapshotReaderFn).apply(eq(createCustomerCmd.getTargetId().getStringValue()));
      verify(cmdHandlerFn).apply(createCustomerCmd, expectedSnapshot);

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.UNKNOWN_COMMAND, response.getResult());

      async.complete();

    });

  }


}