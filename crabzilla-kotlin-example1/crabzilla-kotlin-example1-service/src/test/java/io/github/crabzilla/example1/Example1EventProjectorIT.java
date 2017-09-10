package io.github.crabzilla.example1;

import com.google.inject.Guice;
import io.github.crabzilla.example1.customer.CustomerActivated;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.vertx.projection.EventProjector;
import io.github.crabzilla.vertx.projection.ProjectionData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class Example1EventProjectorIT {

  @Inject
  Jdbi jdbi;
  @Inject
  EventProjector<CustomerSummaryDao> eventProjector;

  @Before
  public void setUp() {

    val vertx = Vertx.vertx();

    JsonObject config = new JsonObject();

    config.put("database.driver", "com.mysql.cj.jdbc.Driver");
    config.put("database.url", "jdbc:mysql://127.0.0.1:3306/example1db?serverTimezone=UTC&useSSL=false");
    config.put("database.user", "root");
    config.put("database.password", "my-secret-pwd");
    config.put("database.pool.max.size", 10);

    log.info("config = {}", config.encodePrettily());

    val injector = Guice.createInjector(new Example1Module(vertx, config));

    injector.injectMembers(this);

    val h = jdbi.open();
    h.createScript("DELETE FROM units_of_work").execute();
    h.createScript("DELETE FROM customer_summary").execute();
    h.commit();

  }


  @Test
  public void canProjectTwoEvents() throws Exception {

    val id = new CustomerId("customer#1");
    val event1 = new CustomerCreated(id,  "customer1");
    val event2 = new CustomerActivated("a good reason", Instant.now());
    val projectionData = new ProjectionData(UUID.randomUUID(), 1L, id.stringValue(), asList(event1, event2));

    eventProjector.handle(singletonList(projectionData));

    val h = jdbi.open();
    val dao = h.attach(CustomerSummaryDao.class);
    val fromDb = dao.getAll().get(0);
    h.commit();

    assertThat(fromDb).isEqualToComparingFieldByField(new CustomerSummary(id.stringValue(), event1.getName(), true));

  }

}
