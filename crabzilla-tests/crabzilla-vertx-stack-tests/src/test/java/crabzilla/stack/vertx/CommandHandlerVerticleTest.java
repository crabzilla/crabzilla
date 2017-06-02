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
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.codecs.gson.CommandCodec;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
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
import javax.inject.Named;
import java.util.UUID;
import java.util.function.Function;

import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static crabzilla.stack.vertx.CommandExecution.RESULT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
  @Inject
  @Named("cmd-handler")
  CircuitBreaker circuitBreaker;

  @Mock
  CommandValidatorFn validatorFn;
  @Mock
  SnapshotReaderFn<Customer> snapshotReaderFn;
  @Mock
  EventRepository eventRepository;
  @Mock
  EventsProjector eventsProjector;

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
        bind(EventsProjector.class).toInstance(eventsProjector);
      }
    })).injectMembers(this);

    val cmdHandler = new CustomerCmdHandlerFnJavaslang(new CustomerStateTransitionFnJavaslang(), dependencyInjectionFn);

    val verticle = new CommandHandlerVerticle<Customer>(Customer.class, snapshotReaderFn, cmdHandler,
                              validatorFn, eventRepository, cache, vertx, circuitBreaker, eventsProjector);

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

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

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

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

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