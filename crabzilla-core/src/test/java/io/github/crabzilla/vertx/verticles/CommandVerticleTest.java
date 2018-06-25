package io.github.crabzilla.vertx.verticles;

import io.github.crabzilla.*;
import io.github.crabzilla.example1.SampleInternalService;
import io.github.crabzilla.example1.customer.*;
import io.github.crabzilla.vertx.CommandExecution;
import io.github.crabzilla.vertx.DbConcurrencyException;
import io.github.crabzilla.vertx.UnitOfWorkRepository;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import net.jodah.expiringmap.ExpiringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.stubbing.VoidAnswer2;
import org.mockito.stubbing.VoidAnswer3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.github.crabzilla.vertx.VertxKt.initVertx;
import static io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandVerticleTest {

  static final String FORCED_CONCURRENCY_EXCEPTION = "FORCED CONCURRENCY EXCEPTION";
  static final String ENTITY_NAME = Customer.class.getSimpleName();

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

  final Customer seedValue =  new Customer(null, null, false, null, service);

  @Mock
  Function1<Command, List<String>> validatorFn;
  @Mock
  Function2<Command, Snapshot<? extends Customer>, CommandResult> cmdHandlerFn;
  @Mock
  UnitOfWorkRepository eventRepository;

  SnapshotPromoter<Customer> snapshotPromoterFn;

  @BeforeEach
  public void setUp(VertxTestContext tc) {

    vertx = Vertx.vertx();

    initVertx(vertx);

    initMocks(this);

    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx);

    cache = ExpiringMap.create();

    final Function1<? super Snapshot<? extends Customer>, StateTransitionsTracker<Customer>> trackerFactory =
            (Function1<Snapshot<? extends Customer>, StateTransitionsTracker<Customer>>) snapshot
                    -> new StateTransitionsTracker<>(snapshot, new StateTransitionFn());

    snapshotPromoterFn = new SnapshotPromoter<Customer>(trackerFactory);

    Verticle verticle = new CommandVerticle<Customer>(ENTITY_NAME,
            seedValue, cmdHandlerFn, validatorFn, snapshotPromoterFn, eventRepository, cache, circuitBreaker);

    vertx.deployVerticle(verticle, tc.succeeding());

    tc.completeNow();

  }

  @Test
  public void SUCCESS_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<>(seedValue, 0);
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    UnitOfWork expectedUow = new UnitOfWork(UUID.randomUUID(), createCustomerCmd, 1, singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class), eq(ENTITY_NAME));

    doAnswer(answerVoid((VoidAnswer2<UnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class), eq(ENTITY_NAME));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(CommandResult.Companion.success(expectedUow));

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any(), eq(ENTITY_NAME));

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any(), eq(ENTITY_NAME));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      assertTrue(asyncResult.succeeded());

      CommandExecution response = (CommandExecution) asyncResult.result().body();

      assertEquals(CommandExecution.RESULT.SUCCESS, response.getResult());
      assertEquals(1L, response.getUowSequence().longValue());

      UnitOfWork uow = response.getUnitOfWork();

      assertEquals(expectedUow.getCommand(), uow.getCommand());
      assertEquals(expectedUow.getEvents(), uow.getEvents());
      assertEquals(expectedUow.getVersion(), uow.getVersion());

      tc.completeNow();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_selectAfterVersion_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, 0);
    Throwable expectedException = new Throwable("Expected");

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.fail(expectedException)))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class), eq(ENTITY_NAME));

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any(), eq(ENTITY_NAME));

      verifyNoMoreInteractions(validatorFn, eventRepository);

      assertTrue(asyncResult.failed());

      ReplyException replyException = (ReplyException) asyncResult.cause();
      assertEquals(replyException.failureCode(), 400);
      assertEquals(replyException.getMessage(), expectedException.getMessage());

      tc.completeNow();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_append_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, 0);
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    UnitOfWork expectedUow = new UnitOfWork(UUID.randomUUID(), createCustomerCmd, 1, singletonList(expectedEvent));
    Throwable expectedException = new Throwable("Expected");

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class), eq(ENTITY_NAME));

    doAnswer(answerVoid((VoidAnswer2<UnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(expectedException)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class), eq(ENTITY_NAME));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(CommandResult.Companion.success(expectedUow));

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any(), eq(ENTITY_NAME));

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any(), eq(ENTITY_NAME));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      assertTrue(asyncResult.failed());

      ReplyException replyException = (ReplyException) asyncResult.cause();
      assertEquals(replyException.failureCode(), 400);
      assertEquals(replyException.getMessage(), expectedException.getMessage());

      tc.completeNow();

    });

  }

  @Test
  public void CONCURRENCY_ERROR_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<>(seedValue, 0);
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    UnitOfWork expectedUow = new UnitOfWork(UUID.randomUUID(), createCustomerCmd, 1, singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class), eq(ENTITY_NAME));

    doAnswer(answerVoid((VoidAnswer2<UnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(new DbConcurrencyException(FORCED_CONCURRENCY_EXCEPTION))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class), eq(ENTITY_NAME));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(CommandResult.Companion.success(expectedUow));

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any(), eq(ENTITY_NAME));

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any(), eq(ENTITY_NAME));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      assertTrue(asyncResult.succeeded());

      CommandExecution response = (CommandExecution) asyncResult.result().body();

      assertEquals(CommandExecution.RESULT.CONCURRENCY_ERROR, response.getResult());
      assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints());

      tc.completeNow();

    });

  }


  @Test
  public void HANDLING_ERROR_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, 0);
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    UnitOfWork expectedUow = new UnitOfWork(UUID.randomUUID(), createCustomerCmd, 1, singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class), eq(ENTITY_NAME));

    doAnswer(answerVoid((VoidAnswer2<UnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class), eq(ENTITY_NAME));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(CommandResult.Companion.error(new RuntimeException("SOME ERROR WITHIN COMMAND HANDLER")));

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any(), eq(ENTITY_NAME));

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      assertTrue(asyncResult.succeeded());

      CommandExecution response = (CommandExecution) asyncResult.result().body();

      assertEquals(CommandExecution.RESULT.HANDLING_ERROR, response.getResult());
      //  TODO inform exception message
      // tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints().get());

      tc.completeNow();

    });

  }

  @Test
  public void VALIDATION_ERROR_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "a bad name");

    List<String> errorList = singletonList("Invalid name: a bad name");
    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(errorList);

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).invoke(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      assertTrue(asyncResult.succeeded());

      CommandExecution response = (CommandExecution) asyncResult.result().body();

      assertEquals(CommandExecution.RESULT.VALIDATION_ERROR, response.getResult());

      assertEquals(asList("Invalid name: a bad name"), response.getConstraints());

      tc.completeNow();

    });

  }


  @Test
  public void UNKNOWN_COMMAND_scenario(VertxTestContext tc) {

    CustomerId customerId = new CustomerId("customer#1");
    UnknownCommand createCustomerCmd = new UnknownCommand(UUID.randomUUID(), customerId);
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, 0);

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Long, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class), eq(ENTITY_NAME));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(null);

    DeliveryOptions options = new DeliveryOptions().setCodecName("Command");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any(), eq(ENTITY_NAME));

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      assertTrue(asyncResult.succeeded());

      CommandExecution response = (CommandExecution) asyncResult.result().body();

      assertEquals(CommandExecution.RESULT.UNKNOWN_COMMAND, response.getResult());

      tc.completeNow();

    });

  }


}
