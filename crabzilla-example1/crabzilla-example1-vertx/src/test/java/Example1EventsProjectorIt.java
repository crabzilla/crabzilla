import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import crabzilla.example1.Example1VertxModule;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.EventsProjector;
import crabzilla.model.ProjectionData;
import crabzilla.stack.EventRepository;
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

@DisplayName("An Example1EventsProjector")
public class Example1EventsProjectorIt {

  @Inject
  ObjectMapper mapper;
  @Inject
  Configuration jooq;
  @Inject
  EventsProjector eventsProjector;

  @BeforeEach
  public void setup() {

    Guice.createInjector(new Example1VertxModule(Vertx.vertx())).injectMembers(this);
    DSL.using(jooq).transaction(ctx -> DSL.using(ctx).execute("DELETE FROM customer_summary"));
  }


  @Test
  public void can_project_two_events() throws EventRepository.DbConcurrencyException {

    val id = new CustomerId("customer#1");
    val event1 = new CustomerCreated(id,  "customer1");
    val event2 = new CustomerActivated("a good reason", Instant.now());
    val projectionData = new ProjectionData(UUID.randomUUID().toString(), 1L, id.getStringValue(), asList(event1, event2));

    eventsProjector.handle(singletonList(projectionData));

    val fromDb = DSL.using(jooq)
            .selectFrom(CUSTOMER_SUMMARY)
            .where(CUSTOMER_SUMMARY.ID.eq(id.getStringValue()))
            .fetchOneInto(example1.datamodel.tables.pojos.CustomerSummary.class);

    assertThat(fromDb).isEqualToComparingFieldByField(new CustomerSummary(id.getStringValue(), event1.getName(), true));

  }

}
