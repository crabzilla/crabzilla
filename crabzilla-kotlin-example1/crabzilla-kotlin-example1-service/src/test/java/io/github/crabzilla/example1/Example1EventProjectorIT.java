package io.github.crabzilla.example1;

import io.github.crabzilla.example1.customer.CustomerActivated;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.example1.customer.CustomerModule;
import io.github.crabzilla.vertx.projection.ProjectionData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class Example1EventProjectorIT {

  Example1Component app ;

  @Before
  public void setUp() {

    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject();

    config.put("database.driver", "com.mysql.cj.jdbc.Driver");
    config.put("database.url", "jdbc:mysql://127.0.0.1:3306/example1db?serverTimezone=UTC&useSSL=false");
    config.put("database.user", "root");
    config.put("database.password", "my-secret-pwd");
    config.put("database.pool.max.size", 10);

    app = DaggerExample1Component.builder()
            .crabzillaModule(new CrabzillaModule(vertx, config))
            .customerModule(new CustomerModule(vertx, config))
            .example1Module(new Example1Module(vertx, config))
            .build();

    Handle h = app.jdbi().open();
    h.createScript("DELETE FROM units_of_work").execute();
    h.createScript("DELETE FROM customer_summary").execute();
    h.commit();

  }


  @Test
  public void canProjectTwoEvents() throws Exception {

    CustomerId id = new CustomerId("customer#1");
    CustomerCreated event1 = new CustomerCreated(id,  "customer1");
    CustomerActivated event2 = new CustomerActivated("a good reason", Instant.now());
    ProjectionData projectionData = new ProjectionData(UUID.randomUUID(), 1L, id.stringValue(), asList(event1, event2));

    app.eventProjector().handle(singletonList(projectionData));

    Handle h = app.jdbi().open();
    CustomerSummaryDao dao = h.attach(CustomerSummaryDao.class);
    CustomerSummary fromDb = dao.getAll().get(0);
    h.commit();

    assertThat(fromDb).isEqualToComparingFieldByField(new CustomerSummary(id.stringValue(), event1.getName(), true));

  }

}
