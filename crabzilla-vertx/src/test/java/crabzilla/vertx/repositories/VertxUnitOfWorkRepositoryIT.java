package crabzilla.vertx.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.vertx.util.DbConcurrencyException;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.VoidTransactionCallback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class VertxUnitOfWorkRepositoryIT {

  static Vertx vertx;
  static JDBCClient jdbcClient;
  static DBI dbi;

  VertxUnitOfWorkRepository repo;

  final CustomerId customerId = new CustomerId("customer#1");
  final CreateCustomerCmd createCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer");
  final CustomerCreated created = new CustomerCreated(createCmd.getTargetId(), "customer");
  final UnitOfWork expectedUow1 = UnitOfWork.unitOfWork(createCmd, new Version(1), singletonList(created));

  final ActivateCustomerCmd activateCmd = new ActivateCustomerCmd(UUID.randomUUID(), customerId, "I want it");
  final CustomerActivated activated = new CustomerActivated(createCmd.getTargetId().getStringValue(), Instant.now());
  final UnitOfWork expectedUow2 = UnitOfWork.unitOfWork(activateCmd, new Version(2), singletonList(activated));

  @BeforeClass
  static public void setupClass(TestContext context) throws IOException, URISyntaxException {

    vertx = Vertx.vertx();

    val mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/example1db?serverTimezone=UTC&useSSL=false");
    config.setUsername("root");
    config.setPassword("my-secret-pwd");
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    val datasource = new HikariDataSource(config) ;

    jdbcClient = JDBCClient.create(vertx, datasource);

    dbi = new DBI(datasource);

    dbi.inTransaction(new VoidTransactionCallback() {
      @Override
      protected void execute(Handle handle, TransactionStatus transactionStatus) throws Exception {
        handle.execute("delete from units_of_work");
        log.info("db is clean");
      }
    });

  }

  @AfterClass
  static public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Before
  public void setup(TestContext context) throws IOException, URISyntaxException {

    this.repo = new VertxUnitOfWorkRepository(Customer.class, jdbcClient);

  }

  @Test
  public void step1_append_version1(TestContext tc) {

    Async async = tc.async();

    repo.append(expectedUow1, appendResult -> {

      appendResult.match(throwable -> {
        fail("error", throwable);
        return null;
      }, (Function<Long, Void>) uowSequence -> {
        assertThat(uowSequence).isGreaterThan(0);
        return null;
      });

      repo.get(expectedUow1.getUnitOfWorkId(), (Optional<UnitOfWork> uow) -> {

        log.debug("uow {}", uow);

        if (uow.isPresent()) {
          assertThat(uow.get()).isEqualTo(expectedUow1);
        } else {
          fail("not found");
        }

        repo.selectAfterVersion(expectedUow1.targetId().getStringValue(), Version.VERSION_ZERO, data -> {

          log.debug("data {}", data);
          assertThat(data.getVersion()).isEqualTo(expectedUow1.getVersion());
          assertThat(data.getEvents()).isEqualTo(expectedUow1.getEvents());

          async.complete();

        });

      });

    });

  }

  @Test
  public void step2_append_version2(TestContext tc) {

    Async async = tc.async();

    repo.append(expectedUow2, appendResult -> {

      appendResult.match(throwable -> {
        fail("error", throwable);
        return null;
      }, (Function<Long, Void>) uowSequence -> {
        assertThat(uowSequence).isGreaterThan(0);
        return null;
      });

      repo.get(expectedUow2.getUnitOfWorkId(), (Optional<UnitOfWork> uow) -> {

        log.debug("uow {}", uow);

        if (uow.isPresent()) {
          assertThat(uow.get()).isEqualTo(expectedUow2);
        } else {
          throw new RuntimeException("not found");
        }

        repo.selectAfterVersion(expectedUow2.targetId().getStringValue(), new Version(1), data -> {

          log.debug("data {}", data);
          assertThat(data.getVersion()).isEqualTo(expectedUow2.getVersion());
          assertThat(data.getEvents()).isEqualTo(expectedUow2.getEvents());

          async.complete();

        });

      });

    });

  }

  @Test
  public void step3_append_version2_again(TestContext tc) {

    Async async = tc.async();

      repo.append(expectedUow2, appendResult -> {

        appendResult.match(throwable -> {
          assertThat(throwable).isInstanceOf(DbConcurrencyException.class);
          return null;
        }, (Function<Long, Void>) uowSequence -> {
          fail("should get DbConcurrencyException");
          return null;
        });

        async.complete();

      });

  }

}
