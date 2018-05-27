package io.github.crabzilla.vertx.projector;

import io.github.crabzilla.DomainEvent;
import io.github.crabzilla.UnitOfWork;
import io.github.crabzilla.example1.CustomerSummary;
import io.github.crabzilla.example1.customer.CreateCustomer;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.vertx.ProjectionData;
import kotlin.jvm.functions.Function2;
import org.jdbi.v3.core.Handle;
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

public class JdbiEventsProjectorTest {

  final static String EVENTS_ENDPOINT = "example1";

  JdbiEventsProjector<CustomerSummaryProjectionDao> eventsProjector;
  @Mock
  Function2<Handle, Class<CustomerSummaryProjectionDao>, CustomerSummaryProjectionDao> daoFactory;
  @Mock
  CustomerSummaryProjectionDao dao;
  @Mock
  Jdbi jdbi;
  @Mock
  Handle handle;

  @Before
  public void setUp() {

    initMocks(this);

    eventsProjector = new JdbiEventsProjector<CustomerSummaryProjectionDao>(EVENTS_ENDPOINT, jdbi, CustomerSummaryProjectionDao.class, daoFactory) {
      @Override
      public void write(CustomerSummaryProjectionDao customerSummaryDao, @NotNull String targetId, @NotNull DomainEvent event) {
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
    UnitOfWork expectedUow =
            new UnitOfWork(UUID.randomUUID(), createCustomerCmd, 1, singletonList(expectedEvent));
    long uowSequence = 1L;

    ProjectionData projectionData = new ProjectionData(expectedUow.getUnitOfWorkId(), uowSequence,
            expectedUow.targetId().stringValue(), expectedUow.getEvents());

    InOrder inOrder = inOrder(jdbi, handle, daoFactory, dao);

    when(jdbi.open()).thenReturn(handle);
    when(daoFactory.invoke(refEq(handle), any())).thenReturn(dao);

    eventsProjector.handle(singletonList(projectionData));

    inOrder.verify(jdbi).open();
    inOrder.verify(handle).begin();
    inOrder.verify(daoFactory).invoke(refEq(handle), any());
    inOrder.verify(dao).insert(eq(new CustomerSummary(customerId.getId(), "customer", false)));
    inOrder.verify(handle).commit();
    inOrder.verify(handle).close();

    verifyNoMoreInteractions(jdbi, handle, daoFactory, dao);

  }

}

