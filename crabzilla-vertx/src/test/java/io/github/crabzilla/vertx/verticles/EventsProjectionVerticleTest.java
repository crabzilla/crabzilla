package io.github.crabzilla.vertx.verticles;

import io.github.crabzilla.example1.customer.CustomerData;
import io.github.crabzilla.example1.readmodel.CustomerSummary;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.model.Version;
import io.github.crabzilla.stack.EventProjector;
import io.github.crabzilla.stack.ProjectionData;
import io.github.crabzilla.stack.StringHelper;
import io.github.crabzilla.vertx.VertxFactory;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.val;
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(VertxUnitRunner.class)
public class EventsProjectionVerticleTest {

  Vertx vertx;
  CircuitBreaker circuitBreaker;

  @Mock
  EventProjector<CustomerSummaryDao> eventProjector;

  @Before
  public void setUp(TestContext context) {

    initMocks(this);

    vertx = new VertxFactory().vertx();
    circuitBreaker = CircuitBreaker.create("cmd-handler-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    val verticle = new EventsProjectionVerticle<CustomerSummaryDao>(eventProjector, circuitBreaker);

    vertx.deployVerticle(verticle, context.asyncAssertSuccess());

  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void must_call_events_projector(TestContext tc) {

    Async async = tc.async();

    val customerId = new CustomerData.CustomerId("customer#1");
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer");
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer");
    val expectedUow = EntityUnitOfWork.unitOfWork(createCustomerCmd, new Version(1), singletonList(expectedEvent));
    val uowSequence = 1L;
    val options = new DeliveryOptions().setCodecName(EntityUnitOfWork.class.getSimpleName())
                                       .addHeader("uowSequence", uowSequence + "");

    val projectionData =
            new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
                    expectedUow.targetId().stringValue(), expectedUow.getEvents());

    vertx.eventBus().send(StringHelper.eventsHandlerId("example1"), expectedUow, options, asyncResult -> {

      verify(eventProjector).handle(eq(asList(projectionData)));

      verifyNoMoreInteractions(eventProjector);

      tc.assertTrue(asyncResult.succeeded());

      async.complete();

    });

  }

  public static interface CustomerSummaryDao {

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

