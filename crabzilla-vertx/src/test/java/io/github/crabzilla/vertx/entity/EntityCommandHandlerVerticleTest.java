package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.*;
import io.github.crabzilla.core.exceptions.DbConcurrencyException;
import io.github.crabzilla.core.exceptions.UnknownCommandException;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.vertx.helpers.StringHelper;
import io.github.crabzilla.vertx.helpers.VertxFactory;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class EntityCommandHandlerVerticleTest {

  public static final String FORCED_CONCURRENCY_EXCEPTION = "FORCED CONCURRENCY EXCEPTION";
  Vertx vertx;
  CircuitBreaker circuitBreaker;
  ExpiringMap<String, Snapshot<Customer>> cache;

  final SampleInternalService service =  new SampleInternalService() {
    @Override
    public UUID uuid() {
      return UUID.randomUUID();
    }

    @Override
    public Instant now() {
      return Instant.now();
    }
  };

  final Customer seedValue =  new Customer(service, null, null, false,null);

  @Mock
  Function<EntityCommand, List<String>> validatorFn;
  @Mock
  BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult> cmdHandlerFn;
  @Mock
  EntityUnitOfWorkRepository eventRepository;
  @Mock
  SnapshotPromoter<Customer> snapshotPromoterFn;

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

    cache = ExpiringMap.create();

    Verticle verticle = new EntityCommandHandlerVerticle<>(Customer.class, seedValue, cmdHandlerFn, validatorFn,
            snapshotPromoterFn, eventRepository, cache, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void SUCCESS_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.success(expectedUow));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      val response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.SUCCESS, response.getResult());
      tc.assertEquals(1L, response.getUowSequence());

      val uow = response.getUnitOfWork();

      if (uow != null) {

        tc.assertEquals(expectedUow.getCommand(), uow.getCommand());
        tc.assertEquals(expectedUow.getEvents(), uow.getEvents());
        tc.assertEquals(expectedUow.getVersion(), uow.getVersion());

      }

      async.complete();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_selectAfterVersion_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    val expectedException = new Throwable("Expected");

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.fail(expectedException)))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
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

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));
    val expectedException = new Throwable("Expected");

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(expectedException)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.success(expectedUow));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
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

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(new DbConcurrencyException(FORCED_CONCURRENCY_EXCEPTION))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.success(expectedUow));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      val response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.CONCURRENCY_ERROR, response.getResult());
      tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints());

      async.complete();

    });

  }


  @Test
  public void HANDLING_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.error(new RuntimeException("SOME ERROR WITHIN COMMAND HANDLER")));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      val response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.HANDLING_ERROR, response.getResult());
      //  TODO inform exception message
      // tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints().get());

      async.complete();

    });

  }

  @Test
  public void VALIDATION_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "a bad name");

    List<String> errorList = singletonList("Invalid name: a bad name");
    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(errorList);

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).apply(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.VALIDATION_ERROR, response.getResult());

      tc.assertEquals(asList("Invalid name: a bad name"), response.getConstraints());

      async.complete();

    });

  }


  @Test
  public void UNKNOWN_COMMAND_scenario(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");
    val createCustomerCmd = new UnknownCommand(UUID.randomUUID(), customerId);
    val initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));

    when(validatorFn.apply(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    when(cmdHandlerFn.apply(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.error(new UnknownCommandException("for command UnknownCommand")));

    val options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).apply(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).apply(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.UNKNOWN_COMMAND, response.getResult());

      async.complete();

    });

  }


}