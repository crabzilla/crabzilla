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
import kotlin.jvm.functions.Function1;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class EventsProjectorTest {

  final static String EVENTS_ENDPOINT = "example1-events";

  EventsProjector<CustomerSummaryDao> eventsProjector;
  @Mock
  Function1<Jdbi, CustomerSummaryDao> daoFactory;
  @Mock
  CustomerSummaryDao dao;
  @Mock
  Jdbi jdbi;

  @Before
  public void setUp() {

    initMocks(this);

    eventsProjector = new EventsProjector<CustomerSummaryDao>(EVENTS_ENDPOINT, jdbi, daoFactory) {
      @Override
      public void write(CustomerSummaryDao customerSummaryDao, @NotNull String targetId, @NotNull DomainEvent event) {
        CustomerCreated e = (CustomerCreated) event;
        dao.insert(new CustomerSummary(targetId, e.getName(), false));
      }
    };
  }

  @Test
  public void must_construct_and_call_dao() {

    CustomerId customerId = new CustomerId("customer#1");
    CreateCustomer createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer");
    EntityUnitOfWork expectedUow =
            new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, new Version(1), singletonList(expectedEvent));
    long uowSequence = 1L;

    ProjectionData projectionData = new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
            expectedUow.targetId().stringValue(), expectedUow.getEvents());


    InOrder inOrder = inOrder(daoFactory, dao);

    when(daoFactory.invoke(refEq(jdbi))).thenReturn(dao);

    eventsProjector.handle(singletonList(projectionData));

    inOrder.verify(daoFactory).invoke(any(Jdbi.class));

    inOrder.verify(dao).insert(eq(new CustomerSummary(customerId.getId(), "customer", false)));

    verifyNoMoreInteractions(daoFactory, dao);

  }

}

