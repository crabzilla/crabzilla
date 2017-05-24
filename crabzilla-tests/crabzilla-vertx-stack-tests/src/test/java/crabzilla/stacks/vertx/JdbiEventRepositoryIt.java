package crabzilla.stacks.vertx;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import crabzilla.example1.Example1VertxModule;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import crabzilla.stacks.vertx.sql.JdbiEventRepository;
import io.vertx.core.Vertx;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.TransactionCallback;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

@DisplayName("A JdbiEventRepository")
public class JdbiEventRepositoryIt {

  @Inject
  Gson gson;
  @Inject
  DBI dbi;

  JdbiEventRepository repo;

  @BeforeEach
  public void setup() {
    Guice.createInjector(Modules.combine(
            new Example1VertxModule(Vertx.vertx())), new AbstractModule() {

      @Override
      protected void configure() {

        final Config config = ConfigFactory.load();
        final Properties props =  new Properties();
        config.entrySet().forEach(e -> {
          final String key = e.getKey().replace("crabzilla-stack1.", "");
          final String value = e.getValue().render().replace("\"", "");
          props.put(key, value);
        });
        Names.bindProperties(binder(), props);

//        bind(new TypeLiteral<Function<Customer, Customer>>() {;}).toInstance(c -> c);

      }
    }).injectMembers(this);

    repo = new JdbiEventRepository("customer", gson, dbi);

    dbi.inTransaction((TransactionCallback<Void>) (handle, transactionStatus) -> {
      handle.execute("delete from idempotency");
      handle.execute("delete from aggregate_roots");
      handle.execute("delete from units_of_work");
      return null;
    });
  }

  @Test
  public void can_append_a_unit_of_work() throws EventRepository.DbConcurrencyException {

    final CustomerId id = new CustomerId("customer#1");
    final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
    final CustomerCreated event = new CustomerCreated(id, command.getName());
    final UnitOfWork uow1 = UnitOfWork.of(command, Version.create(1), Arrays.asList(event));

    repo.append(uow1);

    AssertionsForClassTypes.assertThat(repo.get(uow1.getUnitOfWorkId()).get()).isEqualTo(uow1);

  }
}
