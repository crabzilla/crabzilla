package crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.EventsProjector;
import crabzilla.stack.vertx.JdbiJacksonEventRepository;
import crabzilla.stack.vertx.codecs.JacksonGenericCodec;
import crabzilla.stack.vertx.verticles.CommandExecution;
import crabzilla.stack.vertx.verticles.EventsProjectionVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import lombok.val;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;
import org.nustaq.serialization.FSTConfiguration;
import org.skife.jdbi.v2.DBI;

import javax.sql.DataSource;
import java.util.Properties;

public class Example1VertxModule extends AbstractModule {

  final Vertx vertx;

  public Example1VertxModule(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected void configure() {

    install(new CustomerModule());
    install(new DatabaseModule());

    bind(SampleService.class).to(SampleServiceImpl.class).asEagerSingleton();
    bind(EventsProjectionVerticle.class).asEagerSingleton();

    setCfgProps();

  }

  private void setCfgProps() {

    final Config config = ConfigFactory.load();
    final Properties props =  new Properties();

    config.entrySet().forEach(e -> {
      final String key = e.getKey().replace("crabzilla-stack1.", "");
      final String value = e.getValue().render().replace("\"", "");
      props.put(key, value);
    });

    Names.bindProperties(binder(), props);
  }

  @Provides
  @Singleton
  FSTConfiguration conf() {
    return FSTConfiguration.createDefaultConfiguration();
  }

  @Provides
  @Singleton
  Vertx vertx(ObjectMapper mapper) {

    vertx.eventBus().registerDefaultCodec(CommandExecution.class,
            new JacksonGenericCodec<>(mapper, CommandExecution.class));

    vertx.eventBus().registerDefaultCodec(AggregateRootId.class,
            new JacksonGenericCodec<>(mapper, AggregateRootId.class));

    vertx.eventBus().registerDefaultCodec(Command.class,
            new JacksonGenericCodec<>(mapper, Command.class));

    vertx.eventBus().registerDefaultCodec(Event.class,
            new JacksonGenericCodec<>(mapper, Event.class));

    vertx.eventBus().registerDefaultCodec(UnitOfWork.class,
            new JacksonGenericCodec<>(mapper, UnitOfWork.class));

    return vertx;
  }

  @Provides
  @Singleton
  @Named("cmd-handler")
  CircuitBreaker circuitBreaker() {
    return CircuitBreaker.create("cmd-handler-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

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
  EventRepository eventRepository(ObjectMapper mapper, DBI dbi) {
    return new JdbiJacksonEventRepository(Customer.class.getSimpleName(), mapper, dbi);
  }

  @Provides
  @Singleton
  EventsProjector eventsProjector(Configuration jooq) {
    return new Example1EventsProjector("example1", jooq) ;
  }

  @Provides
  @Singleton
  Configuration cfg(DataSource ds) {
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

  @Provides
  @Singleton
  ObjectMapper mapper() {
    val mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.findAndRegisterModules();
    return mapper;
  }

}
