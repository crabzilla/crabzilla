package crabzilla.example1;

import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.stack.EventProjector;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionRepository;
import crabzilla.stack.StackComponentsFactory;
import crabzilla.stack.vertx.VertxEventRepository;
import crabzilla.stack.vertx.VertxProjectionRepository;
import io.vertx.ext.jdbc.JDBCClient;
import org.jooq.Configuration;

import javax.inject.Inject;

class Example1ComponentsFactory implements StackComponentsFactory {

  private final Configuration jooq;
  private final JDBCClient jdbcClient;

  @Inject
  public Example1ComponentsFactory(Configuration jooq, JDBCClient jdbcClient) {
    this.jooq = jooq;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public EventRepository eventRepository() {
    return new VertxEventRepository(Customer.class.getSimpleName(), jdbcClient);

  }

  @Override
  public EventProjector eventsProjector() {
    return new Example1EventProjector("example1", jooq) ;
  }
;
  @Override
  public ProjectionRepository projectionRepository() {
    return new VertxProjectionRepository(jdbcClient);
  }

}
