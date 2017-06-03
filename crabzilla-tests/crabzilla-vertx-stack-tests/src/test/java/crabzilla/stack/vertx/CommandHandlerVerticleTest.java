package crabzilla.stack.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.codecs.fst.*;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nustaq.serialization.FSTConfiguration;

import java.util.UUID;

import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static crabzilla.stack.vertx.CommandExecution.RESULT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class CommandHandlerVerticleTest {

  static final FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();
  static final SampleService service = new SampleServiceImpl();

  Vertx vertx;
  Cache<String, Snapshot<Customer>> cache;
  CircuitBreaker circuitBreaker;

  @Mock
  CommandValidatorFn validatorFn;
  @Mock
  SnapshotReaderFn<Customer> snapshotReaderFn;
  @Mock
  EventRepository eventRepository;

  @Before
  public void setUp(TestContext context) {

    MockitoAnnotations.initMocks(this);

    vertx = Vertx.vertx();
    vertx.eventBus().registerDefaultCodec(CommandExecution.class, new GenericCodec<>(fst));
    vertx.eventBus().registerDefaultCodec(AggregateRootId.class, new AggregateRootIdCodec(fst));
    vertx.eventBus().registerDefaultCodec(Command.class, new CommandCodec(fst));
    vertx.eventBus().registerDefaultCodec(Event.class, new EventCodec(fst));
    vertx.eventBus().registerDefaultCodec(UnitOfWork.class, new UnitOfWorkCodec(fst));
    cache = Caffeine.newBuilder().build();
    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    val cmdHandler =
            new CustomerCmdHandlerFnJavaslang(new CustomerStateTransitionFnJavaslang(), (c) -> c.withService(service));

    val verticle = new CommandHandlerVerticle<Customer>(Customer.class, snapshotReaderFn, cmdHandler,
                              validatorFn, eventRepository, cache, vertx, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void valid_command_must_result_SUCCESS(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer");

    val expectedMessage = new SnapshotMessage<Customer>(
            new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)),
            SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.constraintViolations(eq(createCustomerCmd))).thenReturn(emptyList());

    when(snapshotReaderFn.getSnapshotMessage(eq(customerId.getStringValue()))).thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName(new CommandCodec(fst).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).constraintViolations(eq(createCustomerCmd));

      verify(snapshotReaderFn).getSnapshotMessage(eq(createCustomerCmd.getTargetId().getStringValue()));

      ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

      verify(eventRepository).append(argument.capture());

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.SUCCESS, response.getResult());

      val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer");
      val expectedUow = UnitOfWork.of(createCustomerCmd, new Version(1), asList(expectedEvent));

      val resultUnitOfWork = response.getUnitOfWork().get();

      tc.assertEquals(expectedUow.getCommand(), resultUnitOfWork.getCommand());
      tc.assertEquals(expectedUow.getEvents(), resultUnitOfWork.getEvents());
      tc.assertEquals(expectedUow.getVersion(), resultUnitOfWork.getVersion());

      async.complete();

    });

  }

  @Test
  public void an_invalid_command_must_result_VALIDATION_ERROR(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val expectedMessage = new SnapshotMessage<Customer>(
            new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)),
            SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.constraintViolations(eq(createCustomerCmd))).thenReturn(singletonList("An error"));

    when(snapshotReaderFn.getSnapshotMessage(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName(new CommandCodec(fst).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).constraintViolations(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, eventRepository);

      tc.assertTrue(asyncResult.succeeded());

      val response = (CommandExecution) asyncResult.result().body();

      tc.assertEquals(RESULT.VALIDATION_ERROR, response.getResult());

      tc.assertEquals(asList("An error"), response.getConstraints().get());

      async.complete();

    });

  }

}