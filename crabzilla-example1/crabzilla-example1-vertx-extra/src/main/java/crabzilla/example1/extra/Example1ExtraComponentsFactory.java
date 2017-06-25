package crabzilla.example1.extra;

import crabzilla.example1.Example1EventProjector;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.model.ProjectionData;
import crabzilla.stack.EventProjector;
import crabzilla.stack.EventRepository;
import crabzilla.stack.StackComponentsFactory;
import crabzilla.stack.vertx.VertxEventRepository;
import crabzilla.stack.vertx.VertxProjectionRepository;
import io.vertx.ext.jdbc.JDBCClient;
import org.jooq.Configuration;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;

public class Example1ExtraComponentsFactory implements StackComponentsFactory {

  private final Configuration jooq;
  private final JDBCClient jdbcClient;

  @Inject
  public Example1ExtraComponentsFactory(Configuration jooq, JDBCClient jdbcClient) {
    this.jooq = jooq;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public EventRepository eventRepository() {
    return new VertxEventRepository(Customer.class, jdbcClient);
  }

  @Override
  public EventProjector eventsProjector() {
    return new Example1EventProjector("example1", jooq) ;
  }

  @Override
  public BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() {
    return new VertxProjectionRepository(jdbcClient);
  }

}
