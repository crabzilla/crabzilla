package crabzilla.stack.vertx;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

public class VertxSqlHelper {

  static void execute(SQLConnection conn, String sql, Handler<Void> done) {
    conn.execute(sql, res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(null);
    });
  }

  static void query(SQLConnection conn, String sql, Handler<ResultSet> done) {
    conn.query(sql, res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(res.result());
    });
  }

  static void queryWithParams(SQLConnection conn, String sql, JsonArray params, Handler<ResultSet> done) {
    conn.queryWithParams(sql, params, res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(res.result());
    });
  }

  static void updateWithParams(SQLConnection conn, String sql, JsonArray params, Handler<UpdateResult> done) {
    conn.updateWithParams(sql, params, res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(res.result());
    });

  }

  static void startTx(SQLConnection conn, Handler<ResultSet> done) {
    conn.setAutoCommit(false, res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(null);
    });
  }

  static void commitTx(SQLConnection conn, Handler<ResultSet> done) {
    conn.commit(res -> {
      if (res.failed()) {
        throw new RuntimeException(res.cause());
      }

      done.handle(null);
    });
  }

}
