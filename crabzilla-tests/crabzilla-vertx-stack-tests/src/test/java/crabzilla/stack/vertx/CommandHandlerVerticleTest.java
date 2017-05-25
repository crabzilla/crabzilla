package crabzilla.stack.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import crabzilla.example1.Example1VertxModule;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.CommandValidatorFn;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.codecs.gson.CommandCodec;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
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

import javax.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class CommandHandlerVerticleTest {

  @Inject
  Vertx vertx;
  @Inject
  Gson gson;
  @Inject
  Function<Customer, Customer> dependencyInjectionFn;

  @Mock
  CommandValidatorFn validatorFn;
  @Mock
  SnapshotReaderFn<Customer> snapshotReaderFn;
  @Mock
  EventRepository eventRepository;

  Cache<String, Snapshot<Customer>> cache = Caffeine.newBuilder().build();

  @Before
  public void setUp(TestContext context) {

    MockitoAnnotations.initMocks(this);
    vertx = Vertx.vertx();
    Guice.createInjector(Modules.override(new Example1VertxModule(vertx)).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(SampleService.class).to(SampleServiceImpl.class).asEagerSingleton();
        bind(new TypeLiteral<SnapshotReaderFn<Customer>>() {;}).toInstance(snapshotReaderFn);
        bind(new TypeLiteral<CommandValidatorFn>() {;}).toInstance(validatorFn);
        bind(EventRepository.class).toInstance(eventRepository);
      }
    })).injectMembers(this);

    val cmdHandler = new CustomerCmdHandlerFnJavaslang(new CustomerStateTransitionFnJavaslang(), dependencyInjectionFn);

    val verticle = new CommandHandlerVerticle<Customer>(Customer.class, snapshotReaderFn, cmdHandler,
                              validatorFn, eventRepository, cache, vertx);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void a_valid_command_must_be_handled(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val expectedMessage = new SnapshotMessage<Customer>(
            new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)),
            SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.constraintViolations(eq(createCustomerCmd))).thenReturn(Collections.emptyList());

    when(snapshotReaderFn.getSnapshotMessage(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

//      System.out.println(asyncResult.failed());
//      System.out.println(asyncResult.cause().getMessage());

      verify(validatorFn).constraintViolations(eq(createCustomerCmd));

      verify(snapshotReaderFn).getSnapshotMessage(eq(createCustomerCmd.getTargetId().getStringValue()));

      val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer1");
      val expectedUow = UnitOfWork.of(createCustomerCmd, new Version(1), Arrays.asList(expectedEvent));

      ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

      verify(eventRepository).append(argument.capture());

      val resultingUow = argument.getValue();

      tc.assertEquals(resultingUow.getCommand(), expectedUow.getCommand());
      tc.assertEquals(resultingUow.getEvents(), expectedUow.getEvents());
      tc.assertEquals(resultingUow.getVersion(), expectedUow.getVersion());

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, eventRepository);

      async.complete();

    });

  }

  @Test
  public void an_invalid_command_must_be_handled(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val expectedMessage = new SnapshotMessage<Customer>(
            new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)),
            SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.constraintViolations(eq(createCustomerCmd))).thenReturn(Collections.singletonList("An error"));

    when(snapshotReaderFn.getSnapshotMessage(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(validatorFn).constraintViolations(eq(createCustomerCmd));

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, eventRepository);

      tc.assertTrue(asyncResult.failed());

      tc.assertEquals("An error\n", asyncResult.cause().getMessage());

      async.complete();

    });

  }

  @Test
  public void multiple_calls_on_aggregate_root(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    val createCustomerCmd = new CreateActivateCustomerCmd(UUID.randomUUID(), customerId,
            "customer1", "because i need two calls on aggregate root");

    val expectedMessage = new SnapshotMessage<Customer>(
            new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)),
            SnapshotMessage.LoadedFromEnum.FROM_DB);

    when(validatorFn.constraintViolations(eq(createCustomerCmd))).thenReturn(Collections.emptyList());

    when(snapshotReaderFn.getSnapshotMessage(eq(customerId.getStringValue())))
            .thenReturn(expectedMessage);

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

//      System.out.println(asyncResult.failed());
//      System.out.println(asyncResult.cause().getMessage());

      verify(validatorFn).constraintViolations(eq(createCustomerCmd));

      verify(snapshotReaderFn).getSnapshotMessage(eq(createCustomerCmd.getTargetId().getStringValue()));

      val expectedEvent1 = new CustomerCreated(createCustomerCmd.getTargetId(), "customer1");
      val expectedEvent2 = new CustomerActivated(createCustomerCmd.getReason(), Instant.now());

      val expectedUow = UnitOfWork.of(createCustomerCmd, new Version(1),
              Arrays.asList(expectedEvent1, expectedEvent2));

      ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

      verify(eventRepository).append(argument.capture());

      val resultingUow = argument.getValue();

      tc.assertEquals(resultingUow.getCommand(), expectedUow.getCommand());
      val event1 = resultingUow.getEvents().get(0);
      CustomerActivated event2 = (CustomerActivated) resultingUow.getEvents().get(1);

      tc.assertEquals(expectedEvent1, event1);
      tc.assertEquals(expectedEvent2.getReason(), event2.getReason());

      tc.assertEquals(resultingUow.getVersion(), expectedUow.getVersion());

      verifyNoMoreInteractions(validatorFn, snapshotReaderFn, eventRepository);

      async.complete();

    });

  }


}