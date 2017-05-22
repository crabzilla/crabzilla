package crabzilla.stacks.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.example1.Example1VertxModule;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stacks.vertx.codecs.CommandCodec;
import crabzilla.stacks.vertx.verticles.CommandHandlerVerticle;
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
import java.util.Arrays;
import java.util.UUID;

import static crabzilla.util.StringHelper.commandHandlerId;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class VertxTest {

  static final Injector injector = Guice.createInjector(new Example1VertxModule());

  @Inject
  Vertx vertx;
  @Inject
  Gson gson;

  @Mock
  SnapshotReaderFn<Customer> snapshotReaderFn;
  @Mock
  EventRepository eventRepository;

  Cache<String, Snapshot<Customer>> cache = Caffeine.newBuilder().build();

  @Before
  public void setUp(TestContext context) {

    MockitoAnnotations.initMocks(this);
    Guice.createInjector(Modules.override(new Example1VertxModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<SnapshotReaderFn<Customer>>() {;}).toInstance(snapshotReaderFn);
        bind(EventRepository.class).toInstance(eventRepository);
        bind(new TypeLiteral<Cache<String, Snapshot<Customer>>>() {;}).toInstance(cache);
      }
    })).injectMembers(this);

    val cmdHandler = new CustomerCmdHandlerFnJavaslang(new CustomerStateTransitionFnJavaslang(), customer -> customer);

    val verticle = new CommandHandlerVerticle<>(Customer.class, snapshotReaderFn, cmdHandler, eventRepository, cache, vertx);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testMyVerticle(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerId("customer#1");

    when(snapshotReaderFn.getSnapshot(eq(customerId.getStringValue())))
            .thenReturn(new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)));

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      verify(snapshotReaderFn).getSnapshot(eq(createCustomerCmd.getTargetId().getStringValue()));

      val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer1");
      val expectedUow = UnitOfWork.of(createCustomerCmd, new Version(1), Arrays.asList(expectedEvent));

      ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

      verify(eventRepository).append(argument.capture());

      val resultingUow = argument.getValue();

      tc.assertEquals(resultingUow.getCommand(), expectedUow.getCommand());
      tc.assertEquals(resultingUow.getEvents(), expectedUow.getEvents());
      tc.assertEquals(resultingUow.getVersion(), expectedUow.getVersion());

      verifyNoMoreInteractions(snapshotReaderFn, eventRepository);

      async.complete();

    });

  }

}