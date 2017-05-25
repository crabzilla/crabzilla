package crabzilla.example1;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
import crabzilla.example1.services.SampleService;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.AggregateRootId;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.stack.EventRepository;
import crabzilla.stack.vertx.codecs.fst.AggregateRootIdCodec;
import crabzilla.stack.vertx.codecs.fst.CommandCodec;
import crabzilla.stack.vertx.codecs.fst.EventCodec;
import crabzilla.stack.vertx.codecs.fst.UnitOfWorkCodec;
import crabzilla.stack.vertx.gson.RuntimeTypeAdapterFactory;
import crabzilla.stack.vertx.sql.JdbiEventRepository;
import io.vertx.core.Vertx;
import lombok.val;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.nustaq.serialization.FSTConfiguration;
import org.skife.jdbi.v2.DBI;

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
  EventRepository eventRepository(Gson gson, DBI dbi) {
    return new JdbiEventRepository(Customer.class.getSimpleName(), gson, dbi);
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
