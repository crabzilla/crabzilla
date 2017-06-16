package crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.stack.EventProjector;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionRepository;
import crabzilla.stack.StackComponentsFactory;
import crabzilla.stack.vertx.JdbiJacksonEventRepository;
import crabzilla.stack.vertx.JdbiJacksonProjectionRepository;
import org.jooq.Configuration;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;

public class Example1ComponentsFactory implements StackComponentsFactory {

  private final Configuration jooq;
  private final ObjectMapper jackson;
  private final DBI jdbi;

  @Inject
  public Example1ComponentsFactory(Configuration jooq, ObjectMapper jackson, DBI jdbi) {
    this.jooq = jooq;
    this.jackson = jackson;
    this.jdbi = jdbi;
  }

  @Override
  public EventRepository eventRepository() {
    return new JdbiJacksonEventRepository(Customer.class.getSimpleName(), jackson, jdbi);

  }

  @Override
  public EventProjector eventsProjector() {
    return new Example1EventProjector("example1", jooq) ;
  }
;
  @Override
  public ProjectionRepository projectionRepository() {
    return new JdbiJacksonProjectionRepository(jackson, jdbi);
  }

}
