package crabzilla.example1.extra;

import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.example1.Example1EventProjector;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.extra.implementations.JdbiJacksonEventRepository;
import crabzilla.example1.extra.implementations.JdbiJacksonProjectionRepository;
import crabzilla.model.ProjectionData;
import crabzilla.stack.EventProjector;
import crabzilla.stack.EventRepository;
import crabzilla.stack.StackComponentsFactory;
import org.jooq.Configuration;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;

public class Example1ExtraComponentsFactory implements StackComponentsFactory {

  private final Configuration jooq;
  private final ObjectMapper jackson;
  private final DBI jdbi;

  @Inject
  public Example1ExtraComponentsFactory(Configuration jooq, ObjectMapper jackson, DBI jdbi) {
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
  public BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() {
    return new JdbiJacksonProjectionRepository(jackson, jdbi);
  }

}
