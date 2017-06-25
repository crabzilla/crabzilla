package crabzilla.example1;

import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.vertx.EventProjector;
import crabzilla.vertx.ProjectionData;
import crabzilla.vertx.VertxBoundedContextComponentsFactory;
import crabzilla.vertx.repositories.VertxEventRepository;
import crabzilla.vertx.repositories.VertxProjectionRepository;
import io.vertx.ext.jdbc.JDBCClient;
import org.jooq.Configuration;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;

class Example1ComponentsFactory implements VertxBoundedContextComponentsFactory {

  private final Configuration jooq;
  private final JDBCClient jdbcClient;

  @Inject
  public Example1ComponentsFactory(Configuration jooq, JDBCClient jdbcClient) {
    this.jooq = jooq;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public VertxEventRepository eventRepository() {
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
