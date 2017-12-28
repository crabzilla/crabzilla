package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.CustomerSummary;
import io.github.crabzilla.example1.customer.CreateCustomer;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.vertx.helpers.VertxFactory;
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle;
import io.github.crabzilla.vertx.projection.EventsProjector;
import io.github.crabzilla.vertx.projection.ProjectionData;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;
import java.util.UUID;

import static io.github.crabzilla.vertx.helpers.StringHelper.projectorEndpoint;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class EventsProjectionVerticleTest {

  Vertx vertx;
  CircuitBreaker circuitBreaker;

  @Mock
  EventsProjector<CustomerSummaryDao> eventsProjector;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = new VertxFactory().vertx();
    circuitBreaker = CircuitBreaker.create(projectorEndpoint(CustomerSummary.class.getSimpleName()), vertx);

    when(eventsProjector.getEventsChannelId()).thenReturn(CustomerSummary.class.getSimpleName());

    EventsProjectionVerticle<CustomerSummaryDao> verticle =
            new EventsProjectionVerticle<>(eventsProjector, circuitBreaker);

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
            EntityUnitOfWork.Companion.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));
    long uowSequence = 1L;
    DeliveryOptions options = new DeliveryOptions().setCodecName(EntityUnitOfWork.class.getSimpleName())
            .addHeader("uowSequence", uowSequence + "");

    ProjectionData projectionData = new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
            expectedUow.targetId().stringValue(), expectedUow.getEvents());


    vertx.eventBus().send(CustomerSummary.class.getSimpleName(), expectedUow, options, asyncResult -> {

      verify(eventsProjector).getEventsChannelId();

      verify(eventsProjector).handle(eq(asList(projectionData)));

      verifyNoMoreInteractions(eventsProjector);

      tc.assertTrue(asyncResult.succeeded());

      async.complete();

    });

  }

  public interface CustomerSummaryDao {

    @SqlUpdate("insert into customer_summary (id, name, is_active) values " +
            "(:id, :name, false)")
    void insert(@BindBean CustomerSummary customerSummary);

    @SqlUpdate("update customer_summary set customer_summary.is_active = :isActive " +
            "where customer_summary.id = :id")
    void updateStatus(@Bind("id") String id, @Bind("isActive") Boolean isActive);

    @SqlQuery("select id, name, is_active from customer_summary")
    @RegisterBeanMapper(CustomerSummary.class)
    List<CustomerSummary> getAll();

  }
}

