//package crabzilla.example1.extra;// TODO move from here to tests module (by decoupling from guice modules)
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.inject.Guice;
//import crabzilla.example1.aggregates.customer.CustomerId;
//import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
//import crabzilla.example1.aggregates.customer.events.CustomerCreated;
//import crabzilla.example1.extra.implementations.JdbiJacksonEventRepository;
//import crabzilla.model.UnitOfWork;
//import crabzilla.model.Version;
//import crabzilla.stack.EventRepository;
//import io.vertx.core.Vertx;
//import lombok.val;
//import org.assertj.core.api.AssertionsForClassTypes;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.skife.jdbi.v2.DBI;
//import org.skife.jdbi.v2.TransactionCallback;
//
//import javax.inject.Inject;
//import java.util.Arrays;
//import java.util.UUID;
//
//@DisplayName("A JdbiJacksonEventRepository")
//public class JdbiJacksonEventRepositoryIt {
//
//  @Inject
//  ObjectMapper mapper;
//  @Inject
//  DBI dbi;
//
//  JdbiJacksonEventRepository repo;
//
//  @BeforeEach
//  public void setup() {
//
//    Guice.createInjector(new Example1ExtraModule(Vertx.vertx())).injectMembers(this);
//
//    repo = new JdbiJacksonEventRepository("customer", mapper, dbi);
//
//    dbi.inTransaction((TransactionCallback<Void>) (handle, transactionStatus) -> {
//      handle.execute("delete from customer_summary");
//      handle.execute("delete from aggregate_roots");
//      handle.execute("delete from units_of_work");
//      return null;
//    });
//  }
//
//  @Test
//  public void can_append_a_unit_of_work() throws EventRepository.DbConcurrencyException {
//
//    val id = new CustomerId("customer#1");
//    val command = new CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
//    val event = new CustomerCreated(id, command.getName());
//    val uow1 = UnitOfWork.unitOfWork(command, Version.create(1), Arrays.asList(event));
//
//    val uowSequence = repo.append(uow1);
//
//    AssertionsForClassTypes.assertThat(0L).isLessThan(uowSequence.longValue());
//    AssertionsForClassTypes.assertThat(repo.get(uow1.getUnitOfWorkId()).get()).isEqualTo(uow1);
//
//  }
//}
