package crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.vertx.EventProjector;
import crabzilla.vertx.ProjectionData;
import crabzilla.vertx.util.DbConcurrencyException;
import example1.datamodel.tables.pojos.CustomerSummary;
import io.vertx.core.Vertx;
import lombok.val;
import org.jooq.Configuration;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static example1.datamodel.tables.CustomerSummary.CUSTOMER_SUMMARY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("An Example1EventProjector")
public class Example1EventProjectorIt {

  @Inject
  ObjectMapper mapper;
  @Inject
  Configuration jooq;
  @Inject
  EventProjector eventProjector;

  @BeforeEach
  public void setup() {

    Guice.createInjector(new Example1Module(Vertx.vertx())).injectMembers(this);
    DSL.using(jooq).transaction(ctx -> DSL.using(ctx).execute("DELETE FROM units_of_work"));
    DSL.using(jooq).transaction(ctx -> DSL.using(ctx).execute("DELETE FROM customer_summary"));
  }


  @Test
  public void can_project_two_events() throws DbConcurrencyException {

    val id = new CustomerId("customer#1");
    val event1 = new CustomerCreated(id,  "customer1");
    val event2 = new CustomerActivated("a good reason", Instant.now());
    val projectionData = new ProjectionData(UUID.randomUUID().toString(), 1L, id.getStringValue(), asList(event1, event2));

    eventProjector.handle(singletonList(projectionData));

    val fromDb = DSL.using(jooq)
            .selectFrom(CUSTOMER_SUMMARY)
            .where(CUSTOMER_SUMMARY.ID.eq(id.getStringValue()))
            .fetchOneInto(example1.datamodel.tables.pojos.CustomerSummary.class);

    assertThat(fromDb).isEqualToComparingFieldByField(new CustomerSummary(id.getStringValue(), event1.getName(), true));

  }

}
