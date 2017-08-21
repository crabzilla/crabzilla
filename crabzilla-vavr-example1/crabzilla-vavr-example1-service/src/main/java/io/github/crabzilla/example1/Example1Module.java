package io.github.crabzilla.example1;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import io.github.crabzilla.example1.customer.CustomerModule;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.example1.services.SampleInternalServiceImpl;
import io.github.crabzilla.model.DomainEvent;
import io.github.crabzilla.model.EntityCommand;
import io.github.crabzilla.model.EntityId;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.stack.CommandExecution;
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec;
import io.github.crabzilla.vertx.verticles.EventsProjectionVerticle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.Properties;

import static io.vertx.core.json.Json.mapper;

class Example1Module extends AbstractModule {

  final Vertx vertx;

  Example1Module(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected void configure() {

    configureVertx();

    // aggregates
    install(new CustomerModule());

    // database
    install(new DatabaseModule());

    // services
    bind(SampleInternalService.class).to(SampleInternalServiceImpl.class).asEagerSingleton();

    // exposes properties to guice
    setCfgProps();

  }

  private void setCfgProps() {

    final Config config = ConfigFactory.load();
    final Properties props = new Properties();

    config.entrySet().forEach(e -> {
      final String key = e.getKey().replace("example1.", "");
      final String value = e.getValue().render().replace("\"", "");
      props.put(key, value);
    });

    Names.bindProperties(binder(), props);
  }

  @Provides
  @Singleton
  Vertx vertx() {
    return vertx;
  }

  @Provides
  @Singleton
  public Jdbi jdbi(HikariDataSource dataSource) {
    val jdbi = Jdbi.create(dataSource);
    jdbi.installPlugin(new SqlObjectPlugin());
//    jdbi.registerRowMapper(ConstructorMapper.factory(CustomerSummary.class)); // TODO how to avoid this ?
    return jdbi;
  }

  @Provides
  @Singleton
  public EventsProjectionVerticle<CustomerSummaryDao> eventsProjector(Jdbi jdbi) {
    val eventsProjector = new Example1EventProjector("example1", CustomerSummaryDao.class, jdbi) ;
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );
    return new EventsProjectionVerticle<>(eventsProjector, circuitBreaker) ;
  }

  @Provides
  @Singleton
  JDBCClient jdbcClient(Vertx vertx, HikariDataSource dataSource) {
    return JDBCClient.create(vertx, dataSource);
  }

//  Not being used yet. This can improve a lot serialization speed (it's binary). But so far it was not necessary.
//  @Provides
//  @Singleton
//  FSTConfiguration conf() {
//    return FSTConfiguration.createDefaultConfiguration();
//  }

  void configureVertx() {

    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    vertx.eventBus().registerDefaultCodec(CommandExecution.class,
            new JacksonGenericCodec<>(mapper, CommandExecution.class));

    vertx.eventBus().registerDefaultCodec(EntityId.class,
            new JacksonGenericCodec<>(mapper, EntityId.class));

    vertx.eventBus().registerDefaultCodec(EntityCommand.class,
            new JacksonGenericCodec<>(mapper, EntityCommand.class));

    vertx.eventBus().registerDefaultCodec(DomainEvent.class,
            new JacksonGenericCodec<>(mapper, DomainEvent.class));

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork.class,
            new JacksonGenericCodec<>(mapper, EntityUnitOfWork.class));

  }

}
