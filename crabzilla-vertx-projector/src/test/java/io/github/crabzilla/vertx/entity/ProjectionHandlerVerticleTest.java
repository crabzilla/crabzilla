package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.CustomerSummary;
import io.github.crabzilla.example1.customer.CreateCustomer;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.vertx.ProjectionData;
import io.github.crabzilla.vertx.projection.EventsProjector;
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import kotlin.jvm.functions.Function1;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.UUID;

import static io.github.crabzilla.vertx.CrabzillaVertxKt.initVertx;
import static io.github.crabzilla.vertx.helpers.EndpointsHelper.projectorEndpoint;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class ProjectionHandlerVerticleTest {

  final static String EVENTS_ENDPOINT = "example1-events";

  Vertx vertx;
  CircuitBreaker circuitBreaker;
  EventsProjector<CustomerSummaryDao> eventsProjector;

  @Mock
  Function1<Jdbi, CustomerSummaryDao> daoFactory;
  @Mock
  CustomerSummaryDao dao;
  @Mock
  Jdbi jdbi;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = Vertx.vertx();

    initVertx(vertx);

    eventsProjector = new EventsProjector<CustomerSummaryDao>(EVENTS_ENDPOINT, jdbi, daoFactory) {
      @Override
      public void write(CustomerSummaryDao customerSummaryDao, @NotNull String targetId, @NotNull DomainEvent event) {
        CustomerCreated e = (CustomerCreated) event;
        dao.insert(new CustomerSummary(targetId, e.getName(), false));
      }
    };

    circuitBreaker = CircuitBreaker.create(projectorEndpoint(CustomerSummary.class.getSimpleName()), vertx);

    ProjectionHandlerVerticle<CustomerSummaryDao> verticle =
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
    EntityUnitOfWork expectedUow =
            new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));
    long uowSequence = 1L;

    DeliveryOptions options = new DeliveryOptions().setCodecName(ProjectionData.class.getSimpleName());

    ProjectionData projectionData = new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
            expectedUow.targetId().stringValue(), expectedUow.getEvents());

    vertx.eventBus().send(EVENTS_ENDPOINT, projectionData, options, asyncResult -> {

      InOrder inOrder = inOrder(daoFactory, dao);

      when(daoFactory.invoke(refEq(jdbi))).thenReturn(dao);

      eventsProjector.handle(singletonList(projectionData));

      inOrder.verify(daoFactory).invoke(refEq(jdbi));

      inOrder.verify(dao).insert(eq(new CustomerSummary(customerId.getId(), "customer", false)));

      tc.assertTrue(asyncResult.succeeded());

      async.complete();

    });

  }

}

