package crabzilla.example1;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.example1.aggregates.customer.events.DeactivatedCmdScheduled;
import crabzilla.example1.projectors.Example1EventsProjectorJooq;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.vertx.CommandHandlingResponse;
import crabzilla.stack.vertx.codecs.fst.*;
import crabzilla.stack.vertx.gson.RuntimeTypeAdapterFactory;
import crabzilla.stack.vertx.sql.JdbiEventRepository;
import crabzilla.stack.vertx.verticles.EventsProjectionVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import lombok.val;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
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
  Vertx vertx(Gson gson, FSTConfiguration fst) {
    vertx.eventBus().registerDefaultCodec(CommandHandlingResponse.class, new GenericCodec<>(fst));
    vertx.eventBus().registerDefaultCodec(AggregateRootId.class, new AggregateRootIdCodec(fst));
    vertx.eventBus().registerDefaultCodec(Command.class, new CommandCodec(fst));
    vertx.eventBus().registerDefaultCodec(Event.class, new EventCodec(fst));
    vertx.eventBus().registerDefaultCodec(UnitOfWork.class, new UnitOfWorkCodec(fst));
//    vertx.eventBus().registerDefaultCodec(AggregateRootId.class, new AggregateRootIdCodec(gson));
//    vertx.eventBus().registerDefaultCodec(Command.class, new CommandCodec(gson));
//    vertx.eventBus().registerDefaultCodec(Event.class, new EventCodec(gson));
//    vertx.eventBus().registerDefaultCodec(UnitOfWork.class, new UnitOfWorkCodec(gson));
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
    //.fallback((error) -> );

  }

  @Provides
  @Singleton
  EventRepository eventRepository(Gson gson, DBI dbi) {
    return new JdbiEventRepository(Customer.class.getSimpleName(), gson, dbi);
  }

  @Provides
  @Singleton
  EventsProjector eventsProjector(Gson gson, Configuration jooq) {
    return new Example1EventsProjectorJooq("example1", jooq) ;
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

  // gson


  @Provides
  @Singleton
  Gson gson() {

    RuntimeTypeAdapterFactory<AggregateRootId> rtaIds  = RuntimeTypeAdapterFactory.of(AggregateRootId.class)
            .registerSubtype(CustomerId.class);

    RuntimeTypeAdapterFactory<Command> rtaCommands  = RuntimeTypeAdapterFactory.of(Command.class)
            .registerSubtype(CreateCustomerCmd.class)
            .registerSubtype(ActivateCustomerCmd.class)
            .registerSubtype(DeactivateCustomerCmd.class)
            .registerSubtype(CreateActivateCustomerCmd.class);

    RuntimeTypeAdapterFactory<Event> rtaEvents = RuntimeTypeAdapterFactory.of(Event.class)
            .registerSubtype(CustomerCreated.class)
            .registerSubtype(CustomerActivated.class)
            .registerSubtype(CustomerDeactivated.class)
            .registerSubtype(DeactivatedCmdScheduled.class);

    val gsonBuilder = new GsonBuilder();

    gsonBuilder.setPrettyPrinting();
    gsonBuilder.registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory());
    gsonBuilder.registerTypeAdapterFactory(rtaIds);
    gsonBuilder.registerTypeAdapterFactory(rtaCommands);
    gsonBuilder.registerTypeAdapterFactory(rtaEvents);

    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES); // snake case

    val gson = gsonBuilder.create();

    return gson;
  }

}
