package crabzilla.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.*;
import crabzilla.vertx.repositories.VertxEventRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Handler;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.stubbing.VoidAnswer2;
import org.mockito.stubbing.VoidAnswer3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.vertx.CommandExecution.RESULT;
import static crabzilla.vertx.util.StringHelper.commandHandlerId;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class CommandHandlerVerticleTest {

  Vertx vertx;
  CircuitBreaker circuitBreaker;

  @Mock
  Cache<String, Snapshot<Customer>> cache;
  @Mock
  Function<Command, List<String>> validatorFn;
  @Mock
  BiFunction<Command, Snapshot<Customer>, Either<Throwable, Optional<UnitOfWork>>> cmdHandlerFn;
  @Mock
  VertxEventRepository eventRepository;
  @Mock
  SnapshotFactory<Customer> snapshotFactory;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = new VertxFactory().vertx();
    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(10000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(false) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    val verticle = new CommandHandlerVerticle<Customer>(Customer.class, cmdHandlerFn,
                              validatorFn, snapshotFactory, eventRepository, cache, vertx, circuitBreaker);

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
    val initialSnapshot = new Snapshot<Customer>(new CustomerSupplierFn().get(), new Version(0));
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = UnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(cache.getIfPresent(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Handler<SnapshotData>>) (s, version, handler) ->
            handler.handle(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Handler.class));

    doAnswer(answerVoid((VoidAnswer2<UnitOfWork, Handler<Long>>) (uow, handler) ->
            handler.handle(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Handler.class));

    when(snapshotFactory.getEmptySnapshot()).thenReturn(initialSnapshot);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.of(expectedUow)));

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

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

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(singletonList("An error"));

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).apply(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

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
    val initialSnapshot = new Snapshot<Customer>(new CustomerSupplierFn().get(), new Version(0));

    when(cache.getIfPresent(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Handler<SnapshotData>>) (s, version, handler) ->
            handler.handle(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
            eq(initialSnapshot.getVersion()),
            any(Handler.class));

    when(snapshotFactory.getEmptySnapshot()).thenReturn(initialSnapshot);

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.empty()));

    val options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.UNKNOWN_COMMAND, response.getResult());

      async.complete();

    });

  }


}