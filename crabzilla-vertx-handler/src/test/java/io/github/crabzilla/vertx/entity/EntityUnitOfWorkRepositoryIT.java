package io.github.crabzilla.vertx.entity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.SnapshotData;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.core.exceptions.DbConcurrencyException;
import io.github.crabzilla.example1.customer.*;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;

import static io.github.crabzilla.vertx.helpers.VertxHelper.initVertx;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EntityUnitOfWorkRepositoryIT {

  private static Logger log = getLogger(EntityUnitOfWorkRepositoryIT.class);

  static Vertx vertx;
  static JDBCClient jdbcClient;
  static Jdbi dbi;

  EntityUnitOfWorkRepository repo;

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
  public void setup(TestContext context) throws IOException, URISyntaxException {

    this.repo = new EntityUnitOfWorkRepository(Customer.class, jdbcClient);

  }

  @Test
  public void step1_append_version1(TestContext tc) {

    Async async = tc.async();

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow1, appendFuture);

    appendFuture.setHandler(appendAsyncResult -> {
      if (appendAsyncResult.failed()) {
        fail("error repo.append", appendAsyncResult.cause());
        return;
      }

      Long uowSequence = appendAsyncResult.result();

      assertThat(uowSequence).isGreaterThan(0);

      Future<EntityUnitOfWork> uowFuture = Future.future();

      repo.getUowByUowId(expectedUow1.getUnitOfWorkId(), uowFuture);

      uowFuture.setHandler(uowAsyncResult -> {
        if (uowAsyncResult.failed()) {
          fail("error repo.get", uowAsyncResult.cause());
          return;
        }

        EntityUnitOfWork uow = uowAsyncResult.result();
        log.debug("uow {}", uow);

        if (uow != null) {
          assertThat(uow).isEqualTo(expectedUow1);
        } else {
          fail("not found");
        }

        Future<SnapshotData> snapshotDataFuture = Future.future();

        repo.selectAfterVersion(expectedUow1.targetId().stringValue(), new Version(0), snapshotDataFuture);

        snapshotDataFuture.setHandler(snapshotDataAsyncResult -> {
          if (snapshotDataAsyncResult.failed()) {
            fail("error repo.selectAfterVersion", snapshotDataAsyncResult.cause());
            return;
          }

          SnapshotData data = snapshotDataAsyncResult.result();
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

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow2, appendFuture);

    appendFuture.setHandler(appendAsyncResult -> {
      if (appendAsyncResult.failed()) {
        fail("error repo.append", appendAsyncResult.cause());
        return;
      }

      Long uowSequence = appendAsyncResult.result();

      assertThat(uowSequence).isGreaterThan(0);

      Future<EntityUnitOfWork> uowFuture = Future.future();

      repo.getUowByUowId(expectedUow2.getUnitOfWorkId(), uowFuture);

      uowFuture.setHandler(uowAsyncResult -> {
        if (uowAsyncResult.failed()) {
          fail("error repo.get", uowAsyncResult.cause());
          return;
        }

        EntityUnitOfWork uow = uowAsyncResult.result();
        log.debug("uow {}", uow);

        if (uow != null) {
          assertThat(uow).isEqualTo(expectedUow2);
        } else {
          fail("not found");
          return;
        }

        Future<SnapshotData> snapshotDataFuture = Future.future();

        repo.selectAfterVersion(expectedUow2.targetId().stringValue(), new Version(1), snapshotDataFuture);

        snapshotDataFuture.setHandler(snapshotDataAsyncResult -> {
          if (snapshotDataAsyncResult.failed()) {
            fail("error repo.selectAfterVersion", snapshotDataAsyncResult.cause());
            return;
          }

          SnapshotData data = snapshotDataAsyncResult.result();
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

    Future<Long> appendFuture = Future.future();

    repo.append(expectedUow2, appendFuture);

    appendFuture.setHandler(appendAsyncResult -> {

      assertThat(appendAsyncResult.cause()).isInstanceOf(DbConcurrencyException.class);

      async.complete();

    });
  }


  @Test
  public void step4_select_version2(TestContext tc) {

    Async async = tc.async();

    Future<SnapshotData> selectFuture = Future.future();

    repo.selectAfterVersion(customerId.stringValue(), new Version(0), selectFuture);

    selectFuture.setHandler(selectAsyncResult -> {

      SnapshotData snapshotData = selectAsyncResult.result();

      assertThat(snapshotData.getVersion()).isEqualTo(new Version(2));
      assertThat(snapshotData.getEvents().get(0)).isEqualTo(created);
      assertThat(snapshotData.getEvents().get(1)).isEqualToIgnoringGivenFields(activated, "_when");
      //TODO problem with Instant serialization

      async.complete();

    });
  }


}
