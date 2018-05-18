package io.github.crabzilla.vertx;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crabzilla.core.SnapshotData;
import io.github.crabzilla.core.UnitOfWork;
import io.github.crabzilla.core.example1.CommandHandlers;
import io.github.crabzilla.example1.customer.*;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static io.github.crabzilla.core.SerializationKt.commandToJson;
import static io.github.crabzilla.core.SerializationKt.listOfEventsToJson;
import static io.github.crabzilla.vertx.VertxKt.initVertx;
import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UnitOfWorkRepositoryIT {

  private static Logger log = Logger.getLogger(UnitOfWorkRepositoryIT.class.getSimpleName());

  static Vertx vertx;
  static JDBCClient jdbcClient;
  static Jdbi dbi;
  static Long seqAfterCreateCmd;
  static Long seqAfterActivateCmd;
  static HikariDataSource datasource;

  static String aggregateId = CommandHandlers.CUSTOMER.name();


  UnitOfWorkRepository repo;

  final CustomerId customerId = new CustomerId("customer#1");
  final CreateCustomer createCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer");
  final CustomerCreated created = new CustomerCreated(customerId, "customer");
  final UnitOfWork expectedUow1 = new UnitOfWork(UUID.randomUUID(), createCmd, 1, singletonList(created));

  final ActivateCustomer activateCmd = new ActivateCustomer(UUID.randomUUID(), customerId, "I want it");
  final CustomerActivated activated = new CustomerActivated(customerId.stringValue(), Instant.now());
  final UnitOfWork expectedUow2 = new UnitOfWork(UUID.randomUUID(), activateCmd, 2, singletonList(activated));


  @ClassRule
  public static final DockerComposeRule docker = DockerComposeRule.builder()
    .file("../docker-compose.yml")
    .removeConflictingContainersOnStartup(true)
    .projectName(new ProjectName() {
      @Override
      protected String projectName() {
        return "crabzilla-UnitOfWorkRepositoryIT";
      }
    })
    .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
    .saveLogsTo("target/dockerComposeRuleTest")
    .build();

  @BeforeClass
  static public void setupClass(TestContext context) throws InterruptedException {

    vertx = Vertx.vertx();

    initVertx(vertx);

    cleanReadDb();

    // TODO move this to test config file
    String WRITE_DATABASE_URL = "jdbc:mysql://127.0.0.1:3306/example1_write?serverTimezone=UTC&useSSL=false";
    String WRITE_DATABASE_DRIVER = "com.mysql.cj.jdbc.Driver";
    String WRITE_DATABASE_USER = "root";
    String WRITE_DATABASE_PASSWORD = "my-secret-pwd";

    HikariConfig config = new HikariConfig();
    config.setDriverClassName(WRITE_DATABASE_DRIVER);
    config.setJdbcUrl(WRITE_DATABASE_URL);
    config.setUsername(WRITE_DATABASE_USER);
    config.setPassword(WRITE_DATABASE_PASSWORD);
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    int attempt= 0;
    while (attempt <= 3) {
      try {
        datasource = new HikariDataSource(config);
        jdbcClient = JDBCClient.create(vertx, datasource);
        dbi = Jdbi.create(datasource);
        cleanWriteDb(datasource);
        break;
      } catch (Exception e) {
        if (++attempt <= 3) {
          log.warning("Failed to access db, will try again");
        } else {
          log.warning("Failed to access db");
        }
        sleep(5_000);
      }
    }

  }

  static void cleanWriteDb(HikariDataSource datasource) {

    Handle h = dbi.open();
    h.createScript("DELETE FROM units_of_work").execute();
    h.commit();
  }

  static void cleanReadDb() throws InterruptedException {

    String READ_DATABASE_URL = "jdbc:mysql://127.0.0.1:3306/example1_read?serverTimezone=UTC&useSSL=false";
    String READ_DATABASE_DRIVER = "com.mysql.cj.jdbc.Driver";
    String READ_DATABASE_USER = "root";
    String READ_DATABASE_PASSWORD = "my-secret-pwd";

    HikariConfig config = new HikariConfig();
    config.setDriverClassName(READ_DATABASE_DRIVER);
    config.setJdbcUrl(READ_DATABASE_URL);
    config.setUsername(READ_DATABASE_USER);
    config.setPassword(READ_DATABASE_PASSWORD);
    config.setAutoCommit(false);
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");

    int attempt= 0;
    while (attempt <= 3) {
      try {
        HikariDataSource datasource = new HikariDataSource(config);
        Jdbi dbi = Jdbi.create(datasource);
        Handle h = dbi.open();
        h.createScript("DELETE FROM customer_summary").execute();
        h.commit();
        break;
      } catch (Exception e) {
        if (++attempt <= 3) {
          log.warning("Failed to access db, will try again");
        } else {
          log.warning("Failed to access db");
        }
        sleep(5_000);
      }
    }

  }

  @AfterClass
  static public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Before
  public void setup(TestContext context) {
    this.repo = new UnitOfWorkRepositoryImpl(jdbcClient);
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

  // --------------------

  @Test
  public void step5_append_version1(TestContext tc) {

    cleanWriteDb(datasource);

    Async async = tc.async();

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow1, appendFuture, aggregateId);

    appendFuture.setHandler(appendAsyncResult -> {
      if (appendAsyncResult.failed()) {
        fail("error repo.append", appendAsyncResult.cause());
        return;
      }

      Long uowSequence = appendAsyncResult.result();

      assertThat(uowSequence).isGreaterThan(0);

      Future<UnitOfWork> uowFuture = Future.future();

      repo.getUowByUowId(expectedUow1.getUnitOfWorkId(), uowFuture);

      uowFuture.setHandler(uowAsyncResult -> {
        if (uowAsyncResult.failed()) {
          fail("error repo.get", uowAsyncResult.cause());
          return;
        }

        UnitOfWork uow = uowAsyncResult.result();
        log.info("uow $uow");

        if (uow != null) {
          assertThat(uow).isEqualTo(expectedUow1);
        } else {
          fail("not found");
        }

        Future<SnapshotData> snapshotDataFuture = Future.future();

        repo.selectAfterVersion(expectedUow1.targetId().stringValue(), 0, snapshotDataFuture, aggregateId);

        snapshotDataFuture.setHandler(snapshotDataAsyncResult -> {
          if (snapshotDataAsyncResult.failed()) {
            fail("error repo.selectAfterVersion", snapshotDataAsyncResult.cause());
            return;
          }

          SnapshotData data = snapshotDataAsyncResult.result();
          log.info("data $data}");
          assertThat(data.getVersion()).isEqualTo(expectedUow1.getVersion());
          assertThat(data.getEvents()).isEqualTo(expectedUow1.getEvents());

          async.complete();

        });
      });
    });
  }

  @Test
  public void step6_append_version2(TestContext tc) {

    Async async = tc.async();

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow2, appendFuture, aggregateId);

    appendFuture.setHandler(appendAsyncResult -> {
      if (appendAsyncResult.failed()) {
        fail("error repo.append", appendAsyncResult.cause());
        return;
      }

      Long uowSequence = appendAsyncResult.result();

      assertThat(uowSequence).isGreaterThan(0);

      Future<UnitOfWork> uowFuture = Future.future();

      repo.getUowByUowId(expectedUow2.getUnitOfWorkId(), uowFuture);

      uowFuture.setHandler(uowAsyncResult -> {
        if (uowAsyncResult.failed()) {
          fail("error repo.get", uowAsyncResult.cause());
          return;
        }

        UnitOfWork uow = uowAsyncResult.result();
        log.info("uow $uow");

        if (uow != null) {
          assertThat(uow).isEqualTo(expectedUow2);
        } else {
          fail("not found");
          return;
        }

        Future<SnapshotData> snapshotDataFuture = Future.future();

        repo.selectAfterVersion(expectedUow2.targetId().stringValue(), 1, snapshotDataFuture, aggregateId);

        snapshotDataFuture.setHandler(snapshotDataAsyncResult -> {
          if (snapshotDataAsyncResult.failed()) {
            fail("error repo.selectAfterVersion", snapshotDataAsyncResult.cause());
            return;
          }

          SnapshotData data = snapshotDataAsyncResult.result();
          log.info("data $data");
          assertThat(data.getVersion()).isEqualTo(expectedUow2.getVersion());
          assertThat(data.getEvents()).isEqualTo(expectedUow2.getEvents());

          async.complete();

        });
      });

    });
  }

  @Test
  public void step7_append_version2_again(TestContext tc) {

    Async async = tc.async();

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow2, appendFuture, aggregateId);

    appendFuture.setHandler(appendAsyncResult -> {

      assertThat(appendAsyncResult.cause()).isInstanceOf(DbConcurrencyException.class);

      async.complete();

    });
  }


  @Test
  public void step8_select_version2(TestContext tc) {

    Async async = tc.async();

    Future<SnapshotData> selectFuture = Future.future();

    repo.selectAfterVersion(customerId.stringValue(), 0, selectFuture, aggregateId);

    selectFuture.setHandler(selectAsyncResult -> {

      SnapshotData snapshotData = selectAsyncResult.result();

      assertThat(snapshotData.getVersion()).isEqualTo(2);
      assertThat(snapshotData.getEvents().get(0)).isEqualTo(created);
      assertThat(snapshotData.getEvents().get(1)).isEqualToIgnoringGivenFields(activated, "_when");

      async.complete();

    });
  }



  // --------------------

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
