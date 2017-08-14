package crabzilla.example1;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.services.SampleInternalService;
import crabzilla.example1.services.SampleInternalServiceImpl;
import crabzilla.model.Command;
import crabzilla.model.EntityId;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.vertx.CommandExecution;
import crabzilla.vertx.EventProjector;
import crabzilla.vertx.codecs.JacksonGenericCodec;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;

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
  public EventProjector eventsProjector(Configuration jooq) {
    return new Example1EventProjector("example1", jooq) ;
  }

  @Provides
  @Singleton
  JDBCClient jdbcClient(Vertx vertx, HikariDataSource dataSource) {
    return JDBCClient.create(vertx, dataSource);
  }

  @Provides
  @Singleton
  @Named("events-projection")
  CircuitBreaker circuitBreakerEvents() {
    return CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

  }

  @Provides
  @Singleton
  Configuration cfg(HikariDataSource ds) {
    DefaultConfiguration cfg = new DefaultConfiguration();
//    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//    cfg.set(() -> new QuerySniper(executor, dbQueryTimeoutMs));
    cfg.set(ds);
    cfg.set(SQLDialect.MYSQL);
    Settings settings = new Settings();
    cfg.setSettings(settings);
    ConnectionProvider cp = new DataSourceConnectionProvider(ds);
    TransactionProvider tp = new DefaultTransactionProvider(cp);
    cfg.setTransactionProvider(tp);
    return cfg;
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

    vertx.eventBus().registerDefaultCodec(Command.class,
            new JacksonGenericCodec<>(mapper, Command.class));

    vertx.eventBus().registerDefaultCodec(Event.class,
            new JacksonGenericCodec<>(mapper, Event.class));

    vertx.eventBus().registerDefaultCodec(UnitOfWork.class,
            new JacksonGenericCodec<>(mapper, UnitOfWork.class));

  }

}
