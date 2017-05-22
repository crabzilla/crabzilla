package crabzilla.example1;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.example1.aggregates.customer.events.DeactivatedCmdScheduled;
import crabzilla.model.AggregateRootId;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.stacks.sql.gson.RuntimeTypeAdapterFactory;
import lombok.val;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class Example1SqlModule extends AbstractModule {

  @Override
  protected void configure() {

  }

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
