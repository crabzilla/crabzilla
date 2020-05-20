package io.github.crabzilla.jooq;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.jooq.example1.datamodel.tables.daos.CustomerSummaryDao;
import io.github.crabzilla.jooq.example1.datamodel.tables.pojos.CustomerSummary;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.crabzilla.jooq.example1.datamodel.Tables.CUSTOMER_SUMMARY;

public class JooqTest {

  public static void main(String[] args) {

    // TODO try another uowProjector using Jooq dao instead of dsl

    Configuration jooq = new DefaultConfiguration();
    jooq.set(SQLDialect.POSTGRES);
    PgPool pgPool = pgPool(Vertx.vertx());

    CustomerSummaryDao dao = new CustomerSummaryDao(jooq, pgPool);
    dao.insert(new CustomerSummary());

//    Function<DSLContext, RowCountQuery> firstOp = dslContext -> dslContext
//      .insertInto(PROJECTIONS)
//      .columns(PROJECTIONS.NAME, PROJECTIONS.LAST_UOW)
//      .values("customer-summary", 1);

    Function<DomainEvent, Function<DSLContext, Query>> f0 = domainEvent -> dsl -> dsl
      .insertInto(CUSTOMER_SUMMARY)
      .columns(CUSTOMER_SUMMARY.ID, CUSTOMER_SUMMARY.NAME, CUSTOMER_SUMMARY.IS_ACTIVE)
      .values(1, "c1", true);

    Function<Function<DSLContext, Query>, BiFunction<ReactiveClassicGenericQueryExecutor, Integer, Future<Integer>>> f1 =
      dslContextQueryFunction -> (tx, integer) -> tx.execute(dslContextQueryFunction);

    List<BiFunction<ReactiveClassicGenericQueryExecutor, Integer, Future<Integer>>> ops = new ArrayList<>();
    ops.add((tx, integer) -> tx.execute(dslContext -> dslContext
      .insertInto(CUSTOMER_SUMMARY)
      .columns(CUSTOMER_SUMMARY.ID, CUSTOMER_SUMMARY.NAME, CUSTOMER_SUMMARY.IS_ACTIVE)
      .values(2, "c2", true)));
    ops.add((tx, integer) -> tx.execute(dslContext -> dslContext.selectFrom(CUSTOMER_SUMMARY)));

    ReactiveClassicGenericQueryExecutor nonTx = new ReactiveClassicGenericQueryExecutor(jooq, pgPool);
//    Future<Integer> resultOfTransaction = nonTx.transaction(tx ->
//      tx.execute(firstOp)
//        .compose(i -> ops.get(0).apply(tx, i))
//        .compose(i -> ops.get(1).apply(tx, i))
//    );

//    resultOfTransaction.onSuccess(res -> System.out.println(res)).onFailure(err -> err.printStackTrace());

  }

  static PgPool pgPool(Vertx vertx) {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("example1")
      .setUser("user1")
      .setPassword("pwd1");
    // Pool options
    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);
    // Create the pooled client
    return PgPool.pool(vertx, connectOptions, poolOptions);
  }
}