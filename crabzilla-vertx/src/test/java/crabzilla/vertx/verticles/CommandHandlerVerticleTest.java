package crabzilla.vertx.verticles;

import crabzilla.example1.customer.Customer;
import crabzilla.example1.customer.CustomerData;
import crabzilla.example1.customer.CustomerFunctions;
import crabzilla.model.*;
import crabzilla.vertx.CommandExecution;
import crabzilla.vertx.VertxFactory;
import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;
import crabzilla.vertx.util.DbConcurrencyException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.Value;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;
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

  public static final String FORCED_CONCURRENCY_EXCEPTION = "FORCED CONCURRENCY EXCEPTION";
  Vertx vertx;
  CircuitBreaker circuitBreaker;

  @Mock
  ExpiringMap<String, Snapshot<Customer>> cache;
  @Mock
  Function<EntityCommand, List<String>> validatorFn;
  @Mock
  BiFunction<EntityCommand, Snapshot<Customer>, Either<Throwable, Optional<EntityUnitOfWork>>> cmdHandlerFn;
  @Mock
  VertxUnitOfWorkRepository eventRepository;
  @Mock
  SnapshotPromoter<Customer> snapshotter;

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
                              validatorFn, snapshotter, eventRepository, cache, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void SUCCESS_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = EntityUnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Either<Throwable, Long>>>) (uow, future) ->
            future.complete(Eithers.right(1L))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.of(expectedUow)));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

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
  public void UNEXPECTED_ERROR_selectAfterVersion_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));
    val expectedException = new Throwable("Expected");

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.fail(expectedException)))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      verifyNoMoreInteractions(validatorFn, eventRepository);

      tc.assertTrue(asyncResult.failed());

      val replyException = (ReplyException) asyncResult.cause();
      tc.assertEquals(replyException.failureCode(), 400);
      tc.assertEquals(replyException.getMessage(), expectedException.getMessage());

      async.complete();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_append_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = EntityUnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));
    val expectedException = new Throwable("Expected");

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Either<Throwable, Long>>>) (uow, future) ->
            future.fail(expectedException)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.of(expectedUow)));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.failed());

      val replyException = (ReplyException) asyncResult.cause();
      tc.assertEquals(replyException.failureCode(), 400);
      tc.assertEquals(replyException.getMessage(), expectedException.getMessage());

      async.complete();

    });

  }

  @Test
  public void CONCURRENCY_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = EntityUnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Either<Throwable, Long>>>) (uow, future) ->
            future.complete(Eithers.left(new DbConcurrencyException(FORCED_CONCURRENCY_EXCEPTION)))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);
    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.of(expectedUow)));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

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

      tc.assertEquals(RESULT.CONCURRENCY_ERROR, response.getResult());
      tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints().get());

      async.complete();

    });

  }


  @Test
  public void HANDLING_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = EntityUnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Either<Throwable, Long>>>) (uow, future) ->
            future.complete(Eithers.right(1L))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.left(new RuntimeException("SOME ERROR WITHIN COMMAND HANDLER")));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.HANDLING_ERROR, response.getResult());
      //  TODO inform exception message
      // tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints().get());

      async.complete();

    });

  }

  @Test
  public void VALIDATION_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer1");

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(singletonList("An error"));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

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
  class UnknownCommand implements EntityCommand {
    UUID commandId;
    CustomerData.CustomerId targetId;
  }

  @Test
  public void UNKNOWN_COMMAND_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new UnknownCommand(UUID.randomUUID(), customerId);
    val initialSnapshot = new Snapshot<Customer>(new CustomerFunctions.SupplierFn().get(), new Version(0));

    when(cache.get(eq(customerId.getStringValue()))).thenReturn(null);
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.getStringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    when(snapshotter.getEmptySnapshot()).thenReturn(initialSnapshot);

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(Eithers.right(Optional.empty()));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

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