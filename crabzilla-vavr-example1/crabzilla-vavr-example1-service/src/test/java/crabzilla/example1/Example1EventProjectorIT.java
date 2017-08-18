package crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import crabzilla.example1.readmodel.CustomerSummary;
import crabzilla.stack.EventProjector;
import crabzilla.stack.ProjectionData;
import io.vertx.core.Vertx;
import lombok.val;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static crabzilla.example1.customer.CustomerData.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("An Example1EventProjector")
public class Example1EventProjectorIT {

  @Inject
  ObjectMapper mapper;
  @Inject
  Jdbi jdbi;
  @Inject
  EventProjector<CustomerSummaryDao> eventProjector;

  @BeforeEach
  public void setup() {

    Guice.createInjector(new Example1Module(Vertx.vertx())).injectMembers(this);

    val h = jdbi.open();
//    h.registerRowMapper(ConstructorMapper.factory(CustomerSummary.class)); // TODO how to avoid this ?
    h.createScript("DELETE FROM units_of_work").execute();
    h.createScript("DELETE FROM customer_summary").execute();
    h.commit();

  }


  @Test
  public void can_project_two_events() throws Exception {

    val id = new CustomerId("customer#1");
    val event1 = new CustomerCreated(id,  "customer1");
    val event2 = new CustomerActivated("a good reason", Instant.now());
    val projectionData = new ProjectionData(UUID.randomUUID(), 1L, id.stringValue(), asList(event1, event2));

    eventProjector.handle(singletonList(projectionData));

    val h = jdbi.open();
    val dao = h.attach(CustomerSummaryDao.class);
    val fromDb = dao.getAll().get(0);
    h.commit();
//    System.out.printf("from  db: " + fromDb);
    assertThat(fromDb).isEqualToComparingFieldByField(new CustomerSummary(id.stringValue(), event1.getName(), true));

  }

}
