package crabzilla.vertx.repositories;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import io.vertx.ext.sql.UpdateResult;

class VertxSqlHelper {

  static void execute(SQLConnection conn, String sql, Future<Void> future) {
    conn.execute(sql, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(null);
    });
  }

  static void query(SQLConnection conn, String sql, Future<ResultSet> future) {
    conn.query(sql, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(res.result());
    });
  }

  static void queryWithParams(SQLConnection conn, String sql, JsonArray params, Future<ResultSet> future) {
    conn.queryWithParams(sql, params, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(res.result());
    });
  }

  static void queryStreamWithParams(SQLConnection conn, String sql, JsonArray params, Future<SQLRowStream> future) {
    conn.queryStreamWithParams(sql, params, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(res.result());
    });
  }
  static void updateWithParams(SQLConnection conn, String sql, JsonArray params, Future<UpdateResult> future) {
    conn.updateWithParams(sql, params, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(res.result());
    });

  }

  static void startTx(SQLConnection conn, Future<Void> future) {
    conn.setAutoCommit(false, res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(null);
    });
  }

  static void commitTx(SQLConnection conn, Future<Void> future) {
    conn.commit(res -> {
      if (res.failed()) {
        future.fail(res.cause());
        return;
      }

      future.complete(null);
    });
  }
}
