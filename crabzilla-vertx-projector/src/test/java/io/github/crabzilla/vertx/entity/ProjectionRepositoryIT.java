package io.github.crabzilla.vertx.entity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.Optional;
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
  static Long seqAfterCreateCmd;
  static Long seqAfterActivateCmd;

  ProjectionRepository repo;

  final CustomerId customerId = new CustomerId("customer#1");
  final CreateCustomer createCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
  final CustomerCreated created = new CustomerCreated(customerId, "customer");

  final ActivateCustomer activateCmd = new ActivateCustomer(UUID.randomUUID(), customerId, "I want it");
  final CustomerActivated activated = new CustomerActivated(customerId.stringValue(), Instant.now());

  @BeforeClass
  static public void setupClass(TestContext context) {

    vertx = Vertx.vertx();

    initVertx(vertx);

    cleanReadDb();

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/example1_write?serverTimezone=UTC&useSSL=false");
    config.setUsername("root");
    config.setPassword("my-secret-pwd");
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    HikariDataSource datasource = new HikariDataSource(config);

    dbi = Jdbi.create(datasource);

    cleanWriteDb(datasource);

    jdbcClient = JDBCClient.create(vertx, datasource);

  }

  static void cleanWriteDb(HikariDataSource datasource) {

    Handle h = dbi.open();
    h.createScript("DELETE FROM units_of_work").execute();
    h.commit();
  }

  static void cleanReadDb() {

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/example1_read?serverTimezone=UTC&useSSL=false");
    config.setUsername("root");
    config.setPassword("my-secret-pwd");
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    HikariDataSource datasource = new HikariDataSource(config);
    Jdbi _dbi = Jdbi.create(datasource);

    Handle h = _dbi.open();
    h.createScript("DELETE FROM customer_summary").execute();
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

    repo.selectAfterUowSequence(0L, 100, selectFuture);

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

    seqAfterCreateCmd = populateAfterCreateCommand().get();

    repo.selectAfterUowSequence(0L, 100, selectFuture);

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


  @Test
  public void step3_select_only_uow_1(TestContext tc) {

    Async async = tc.async();

    Future<List<ProjectionData>> selectFuture = Future.future();

    seqAfterActivateCmd = populateAfterActivateCommand().get();

    repo.selectAfterUowSequence(seqAfterCreateCmd, 100, selectFuture);

    selectFuture.setHandler(selectAsyncResult -> {

      List<ProjectionData> snapshotData = selectAsyncResult.result();

      assertThat(snapshotData.size()).isEqualTo(1);

      ProjectionData pd1 = snapshotData.get(0);

      assertThat(pd1.getUowSequence()).isGreaterThan(0);
      assertThat(pd1.getTargetId()).isEqualTo(customerId.stringValue());
      assertThat(pd1.getEvents()).isNotEqualTo(
        listOfEventsToJson(Json.mapper, singletonList(activated)));

      async.complete();

    });
  }


  @Test
  public void step4_select_all(TestContext tc) {

    Async async = tc.async();

    Future<List<ProjectionData>> selectFuture = Future.future();

    repo.selectAfterUowSequence(0L, 100, selectFuture);

    selectFuture.setHandler(selectAsyncResult -> {

      List<ProjectionData> snapshotData = selectAsyncResult.result();

      assertThat(snapshotData.size()).isEqualTo(2);

      ProjectionData pd1 = snapshotData.get(0);

      assertThat(pd1.getUowSequence()).isGreaterThan(0);
      assertThat(pd1.getTargetId()).isEqualTo(customerId.stringValue());
      assertThat(pd1.getEvents()).isNotEqualTo(
        listOfEventsToJson(Json.mapper, singletonList(created)));

      ProjectionData pd2 = snapshotData.get(1);

      assertThat(pd2.getUowSequence()).isGreaterThan(0);
      assertThat(pd2.getTargetId()).isEqualTo(customerId.stringValue());
      assertThat(pd2.getEvents()).isNotEqualTo(
        listOfEventsToJson(Json.mapper, singletonList(activated)));

      async.complete();

    });
  }

  Optional<Long> populateAfterCreateCommand() {

    Handle h = dbi.open();

    Optional<Long> seq = h.createUpdate("INSERT INTO units_of_work" +
      "(uow_id, uow_events, cmd_id, cmd_data, ar_name, ar_id, version, inserted_on)" +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
      .bind(0, UUID.randomUUID().toString())
      .bind(1, listOfEventsToJson(Json.mapper, singletonList(created)))
      .bind(2, createCmd.component1().toString())
      .bind(3, commandToJson(Json.mapper, createCmd))
      .bind(4, Customer.class.getSimpleName())
      .bind(5, customerId.stringValue())
      .bind(6, 1)
      .bind(7, Instant.now())
      .executeAndReturnGeneratedKeys()
      .mapTo(Long.class).findFirst();

    h.commit();

    return seq ;
  }

  Optional<Long>  populateAfterActivateCommand() {

    Handle h = dbi.open();

    Optional<Long> seq = h.createUpdate("INSERT INTO units_of_work" +
      "(uow_id, uow_events, cmd_id, cmd_data, ar_name, ar_id, version, inserted_on)" +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
      .bind(0, UUID.randomUUID().toString())
      .bind(1, listOfEventsToJson(Json.mapper, singletonList(activated)))
      .bind(2, activateCmd.component1().toString())
      .bind(3, commandToJson(Json.mapper, activateCmd))
      .bind(4, Customer.class.getSimpleName())
      .bind(5, customerId.stringValue())
      .bind(6, 1)
      .bind(7, Instant.now())
      .executeAndReturnGeneratedKeys()
      .mapTo(Long.class).findFirst();

    h.commit();

    return seq;

  }
}
