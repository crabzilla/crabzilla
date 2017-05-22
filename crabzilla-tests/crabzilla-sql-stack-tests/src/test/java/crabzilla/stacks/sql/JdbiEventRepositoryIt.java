package crabzilla.stacks.sql;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.example1.DatabaseModule;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.stack.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.TransactionCallback;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DisplayName("A JdbiEventRepository")
public class JdbiEventRepositoryIt {

  final static Injector injector = Guice.createInjector(
          new DatabaseModule(),
          new CustomerModule());

  @Inject
  Gson gson;
  @Inject
  DBI dbi;

  JdbiEventRepository repo;

  @BeforeEach
  public void setup() {
    injector.injectMembers(this);// TODO vai ter que voltar o example1-data-stack
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

    assertThat(repo.get(uow1.getUnitOfWorkId()).get()).isEqualTo(uow1);

  }
}
