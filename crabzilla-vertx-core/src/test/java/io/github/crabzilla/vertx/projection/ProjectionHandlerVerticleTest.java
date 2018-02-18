package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.UnitOfWork;
import io.github.crabzilla.core.Version;
import io.github.crabzilla.example1.CustomerSummary;
import io.github.crabzilla.example1.customer.CreateCustomer;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import kotlin.jvm.functions.Function2;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.UUID;

import static io.github.crabzilla.core.example1.Example1Kt.subDomainName;
import static io.github.crabzilla.vertx.VertxKt.initVertx;
import static io.github.crabzilla.vertx.helpers.EndpointsHelper.projectorEndpoint;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class ProjectionHandlerVerticleTest {

  final static String EVENTS_ENDPOINT = subDomainName();

  Vertx vertx;
  CircuitBreaker circuitBreaker;
  AbstractEventsProjector<CustomerSummaryProjectionDao> eventsProjector;

  @Mock
  Function2<Handle, Class<CustomerSummaryProjectionDao>, CustomerSummaryProjectionDao> daoFactory;
  @Mock
  CustomerSummaryProjectionDao dao;
  @Mock
  Jdbi jdbi;
  @Mock
  Handle handle;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = Vertx.vertx();

    initVertx(vertx);

    when(jdbi.open()).thenReturn(handle);
    when(daoFactory.invoke(refEq(handle), any())).thenReturn(dao);

    eventsProjector = new AbstractEventsProjector<CustomerSummaryProjectionDao>(EVENTS_ENDPOINT, jdbi, CustomerSummaryProjectionDao.class, daoFactory) {
      @Override
      public void write(CustomerSummaryProjectionDao customerSummaryDao, @NotNull String targetId, @NotNull DomainEvent event) {
        CustomerCreated e = (CustomerCreated) event;
        dao.insert(new CustomerSummary(targetId, e.getName(), false));
      }
    };

    circuitBreaker = CircuitBreaker.create(projectorEndpoint(CustomerSummary.class.getSimpleName()), vertx);

    ProjectionHandlerVerticle<CustomerSummaryProjectionDao> verticle =
            new ProjectionHandlerVerticle<>(EVENTS_ENDPOINT, eventsProjector, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void must_call_events_projector(TestContext tc) {

    Async async = tc.async();

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    UnitOfWork expectedUow =
            new UnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));
    long uowSequence = 1L;

    DeliveryOptions options = new DeliveryOptions().setCodecName(ProjectionData.class.getSimpleName());

    ProjectionData projectionData = new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
            expectedUow.targetId().stringValue(), expectedUow.getEvents());

    vertx.eventBus().send(EVENTS_ENDPOINT, projectionData, options, asyncResult -> {

      InOrder inOrder = inOrder(jdbi, handle, daoFactory, dao);

      eventsProjector.handle(singletonList(projectionData));

      inOrder.verify(jdbi).open();
      inOrder.verify(handle).begin();
      inOrder.verify(daoFactory).invoke(any(), any());
      inOrder.verify(dao).insert(eq(new CustomerSummary(customerId.getId(), "customer", false)));
      inOrder.verify(handle).commit();
      inOrder.verify(handle).close();

//      inOrder.verifyNoMoreInteractions();

      tc.assertTrue(asyncResult.succeeded());

      async.complete();

    });

  }

}

