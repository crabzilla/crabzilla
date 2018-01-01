package io.github.crabzilla.vertx.entity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.customer.*;
import io.github.crabzilla.vertx.ProjectionData;
import io.github.crabzilla.vertx.projection.ProjectionRepository;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.github.crabzilla.core.KrabzillaKt.commandToJson;
import static io.github.crabzilla.core.KrabzillaKt.listOfEventsToJson;
import static io.github.crabzilla.vertx.CrabzillaVertxKt.initVertx;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectionRepositoryIT {

  private static Logger log = getLogger(ProjectionRepositoryIT.class);

  static Vertx vertx;
  static JDBCClient jdbcClient;
  static Jdbi dbi;

  ProjectionRepository repo;

  final CustomerId customerId = new CustomerId("customer#1");
  final CreateCustomer createCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
  final CustomerCreated created = new CustomerCreated(customerId, "customer");
  final EntityUnitOfWork expectedUow1 = new EntityUnitOfWork(UUID.randomUUID(), createCmd, new Version(1), singletonList(created));

  final ActivateCustomer activateCmd = new ActivateCustomer(UUID.randomUUID(), customerId, "I want it");
  final CustomerActivated activated = new CustomerActivated(customerId.stringValue(), Instant.now());
  final EntityUnitOfWork expectedUow2 = new EntityUnitOfWork(UUID.randomUUID(), activateCmd, new Version(2), singletonList(activated));

  @BeforeClass
  static public void setupClass(TestContext context) {

    vertx = Vertx.vertx();

    initVertx(vertx);

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/example1_write?serverTimezone=UTC&useSSL=false");
    config.setUsername("root");
    config.setPassword("my-secret-pwd");
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    HikariDataSource datasource = new HikariDataSource(config);;

    jdbcClient = JDBCClient.create(vertx, datasource);

    dbi = Jdbi.create(datasource);

    Handle h = dbi.open();
    h.createScript("DELETE FROM units_of_work").execute();
//    h.createScript("DELETE FROM customer_summary").execute();
    h.commit();

  }

  @AfterClass
  static public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Before
  public void setup(TestContext context) {
    this.repo = new ProjectionRepository(jdbcClient);
  }

  @Test
  public void step1_select_empty(TestContext tc) {

    Async async = tc.async();

    Future<List<ProjectionData>> selectFuture = Future.future();

    repo.selectAfterUowSequence(0L, 0, selectFuture);

    selectFuture.setHandler(selectAsyncResult -> {

      List<ProjectionData> snapshotData = selectAsyncResult.result();
      assertThat(snapshotData.size()).isEqualByComparingTo(0);

      async.complete();

    });
  }

  @Test
  public void step2_select_uow_1(TestContext tc) {

    Async async = tc.async();

    Future<List<ProjectionData>> selectFuture = Future.future();

    populateAfterCreateCommand();

    repo.selectAfterUowSequence(0L, selectFuture);

    selectFuture.setHandler(selectAsyncResult -> {

      List<ProjectionData> snapshotData = selectAsyncResult.result();

      assertThat(snapshotData.size()).isEqualTo(1);

      ProjectionData pd1 = snapshotData.get(0);

      assertThat(pd1.getUowSequence()).isGreaterThan(0);
      assertThat(pd1.getTargetId()).isEqualTo(customerId.stringValue());
      assertThat(pd1.getEvents()).isNotEqualTo(
              listOfEventsToJson(Json.mapper, singletonList(created)));

      async.complete();

    });
  }

  void populateAfterCreateCommand() {

    Handle h = dbi.open();

    h.createUpdate("INSERT INTO units_of_work" +
            "(uow_id, uow_events, cmd_id, cmd_data, ar_name, ar_id, version, inserted_on)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
            .bind(0, UUID.randomUUID().toString())
            .bind(1, listOfEventsToJson(Json.mapper, singletonList(created)))
            .bind(2, createCmd.component1().toString())
            .bind(3, commandToJson(Json.mapper,createCmd))
            .bind(4, Customer.class.getSimpleName())
            .bind(5, customerId.stringValue())
            .bind(6, 1)
            .bind(7, Instant.now())
            .execute();

    h.commit();

  }

}
