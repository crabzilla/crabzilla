package crabzilla.example1;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import crabzilla.UnitOfWork;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.CustomerVertxModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stacks.sql.JdbiEventRepository;
import crabzilla.stacks.vertx.codecs.AggregateRootIdCodec;
import crabzilla.stacks.vertx.codecs.CommandCodec;
import crabzilla.stacks.vertx.codecs.EventCodec;
import crabzilla.stacks.vertx.codecs.UnitOfWorkCodec;
import crabzilla.stacks.vertx.verticles.CommandHandlerVerticle;
import io.vertx.core.Vertx;
import org.skife.jdbi.v2.DBI;

import java.util.Properties;

public class Example1VertxModule extends AbstractModule {

  @Override
  protected void configure() {

    install(new CustomerModule());
    install(new Example1SqlModule());
    install(new CustomerVertxModule());
    install(new DatabaseModule());

    bind(SampleService.class).to(SampleServiceImpl.class).asEagerSingleton();

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
  Vertx vertx(Gson gson) {
    final Vertx vertx = Vertx.vertx();
    vertx.eventBus().registerDefaultCodec(AggregateRootId.class, new AggregateRootIdCodec(gson));
    vertx.eventBus().registerDefaultCodec(Command.class, new CommandCodec(gson));
    vertx.eventBus().registerDefaultCodec(Event.class, new EventCodec(gson));
    vertx.eventBus().registerDefaultCodec(UnitOfWork.class, new UnitOfWorkCodec(gson));
    return vertx;
  }

  @Provides
  @Singleton
  EventRepository eventRepository(Gson gson, DBI dbi) {
    return new JdbiEventRepository(Customer.class.getSimpleName(), gson, dbi);
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(SnapshotReaderFn<Customer> snapshotReaderFn,
                                           CommandHandlerFn<Customer> cmdHandler,
                                           CommandValidatorFn validatorFn,
                                           EventRepository eventStore, Vertx vertx,
                                           Cache<String, Snapshot<Customer>> cache) {
    return
      new CommandHandlerVerticle<>(Customer.class, snapshotReaderFn, cmdHandler, validatorFn, eventStore, cache, vertx);
  }

}
