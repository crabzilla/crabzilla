package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.UnknownCommandException;
import io.github.crabzilla.core.entity.*;
import io.github.crabzilla.example1.SampleInternalService;
import io.github.crabzilla.example1.customer.*;
import io.github.crabzilla.vertx.DbConcurrencyException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
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

import static io.github.crabzilla.vertx.CrabzillaVertxKt.initVertx;
import static io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class EntityCommandHandlerVerticleTest {

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
  Function1<EntityCommand, List<String>> validatorFn;
  @Mock
  Function2<EntityCommand, Snapshot<? extends Customer>, EntityCommandResult> cmdHandlerFn;
  @Mock
  EntityUnitOfWorkRepository eventRepository;

  SnapshotPromoter<Customer> snapshotPromoterFn;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = Vertx.vertx();

    initVertx(vertx);

    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx);

    cache = ExpiringMap.create();

    final Function1<? super Snapshot<? extends Customer>, StateTransitionsTracker<Customer>> trackerFactory =
            (Function1<Snapshot<? extends Customer>, StateTransitionsTracker<Customer>>) snapshot
                    -> new StateTransitionsTracker<>(snapshot, new StateTransitionFn());

    snapshotPromoterFn = new SnapshotPromoter<Customer>(trackerFactory);

    Verticle verticle = new EntityCommandHandlerVerticle<Customer>(ENTITY_NAME,
            seedValue, cmdHandlerFn, validatorFn, snapshotPromoterFn, eventRepository, cache, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void SUCCESS_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<>(seedValue, new Version(0));
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    EntityUnitOfWork expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.Companion.success(expectedUow));

//    when(snapshotPromoterFn.promote(any(Snapshot.class), eq(new Version(1)), eq(singletonList(expectedEvent))))
//            .thenReturn(finalSnapshot);

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      EntityCommandExecution response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.SUCCESS, response.getResult());
      tc.assertEquals(1L, response.getUowSequence());

      EntityUnitOfWork uow = response.getUnitOfWork();

      tc.assertEquals(expectedUow.getCommand(), uow.getCommand());
      tc.assertEquals(expectedUow.getEvents(), uow.getEvents());
      tc.assertEquals(expectedUow.getVersion(), uow.getVersion());

      async.complete();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_selectAfterVersion_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    Throwable expectedException = new Throwable("Expected");

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.fail(expectedException)))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());
//
      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      verifyNoMoreInteractions(validatorFn, eventRepository);

      tc.assertTrue(asyncResult.failed());

      ReplyException replyException = (ReplyException) asyncResult.cause();
      tc.assertEquals(replyException.failureCode(), 400);
      tc.assertEquals(replyException.getMessage(), expectedException.getMessage());

      async.complete();

    });

  }

  @Test
  public void UNEXPECTED_ERROR_append_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    EntityUnitOfWork expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));
    Throwable expectedException = new Throwable("Expected");

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                      eq(initialSnapshot.getVersion()),
                                                      any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(expectedException)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.Companion.success(expectedUow));

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
                                                         eq(initialSnapshot.getVersion()),
                                                         any());

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.failed());

      ReplyException replyException = (ReplyException) asyncResult.cause();
      tc.assertEquals(replyException.failureCode(), 400);
      tc.assertEquals(replyException.getMessage(), expectedException.getMessage());

      async.complete();

    });

  }

  @Test
  public void CONCURRENCY_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<>(seedValue, new Version(0));
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    EntityUnitOfWork expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.fail(new DbConcurrencyException(FORCED_CONCURRENCY_EXCEPTION))))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.Companion.success(expectedUow));

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      inOrder.verify(eventRepository).append(eq(expectedUow), any());

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      EntityCommandExecution response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.CONCURRENCY_ERROR, response.getResult());
      tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints());

      async.complete();

    });

  }


  @Test
  public void HANDLING_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    EntityUnitOfWork expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    doAnswer(answerVoid((VoidAnswer2<EntityUnitOfWork, Future<Long>>) (uow, future) ->
            future.complete(1L)))
            .when(eventRepository).append(eq(expectedUow), any(Future.class));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.Companion.error(new RuntimeException("SOME ERROR WITHIN COMMAND HANDLER")));

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, eventRepository, cmdHandlerFn);

      tc.assertTrue(asyncResult.succeeded());

      EntityCommandExecution response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.HANDLING_ERROR, response.getResult());
      //  TODO inform exception message
      // tc.assertEquals(singletonList(FORCED_CONCURRENCY_EXCEPTION), response.getConstraints().get());

      async.complete();

    });

  }

  @Test
  public void VALIDATION_ERROR_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "a bad name");

    List<String> errorList = singletonList("Invalid name: a bad name");
    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(errorList);

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).invoke(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      EntityCommandExecution response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.VALIDATION_ERROR, response.getResult());

      tc.assertEquals(asList("Invalid name: a bad name"), response.getConstraints());

      async.complete();

    });

  }


  @Test
  public void UNKNOWN_COMMAND_scenario(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    UnknownCommand createCustomerCmd = new UnknownCommand(UUID.randomUUID(), customerId);
    Snapshot<Customer> initialSnapshot = new Snapshot<Customer>(seedValue, new Version(0));

    when(validatorFn.invoke(eq(createCustomerCmd))).thenReturn(emptyList());

    doAnswer(answerVoid((VoidAnswer3<String, Version, Future<SnapshotData>>) (s, version, future) ->
            future.complete(new SnapshotData(initialSnapshot.getVersion(), new ArrayList<>()))))
            .when(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
            eq(initialSnapshot.getVersion()),
            any(Future.class));

    when(cmdHandlerFn.invoke(eq(createCustomerCmd), eq(initialSnapshot)))
            .thenReturn(EntityCommandResult.Companion.error(new UnknownCommandException("for command UnknownCommand")));

    DeliveryOptions options = new DeliveryOptions().setCodecName("EntityCommand");

    vertx.eventBus().send(cmdHandlerEndpoint(ENTITY_NAME), createCustomerCmd, options, asyncResult -> {

      InOrder inOrder = inOrder(validatorFn, eventRepository, cmdHandlerFn);

      inOrder.verify(validatorFn).invoke(eq(createCustomerCmd));

//      inOrder.verify(eventRepository).getUowByCmdId(eq(createCustomerCmd.getCommandId()),
//              any());

      inOrder.verify(eventRepository).selectAfterVersion(eq(customerId.stringValue()),
              eq(initialSnapshot.getVersion()),
              any());

      inOrder.verify(cmdHandlerFn).invoke(eq(createCustomerCmd), eq(initialSnapshot));

      verifyNoMoreInteractions(validatorFn, cmdHandlerFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      EntityCommandExecution response = (EntityCommandExecution) asyncResult.result().body();

      tc.assertEquals(EntityCommandExecution.RESULT.UNKNOWN_COMMAND, response.getResult());

      async.complete();

    });

  }


}
