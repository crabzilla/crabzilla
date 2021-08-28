package io.github.crabzilla.engine

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet

fun AsyncResult<RowSet<Row>>.assertAffectedRows(expectedRows: Int) : Future<Void> {
  return if (this.succeeded() && this.result().rowCount() == expectedRows) {
    Future.succeededFuture()
  } else {
    Future.failedFuture("Expected rows $expectedRows but it was ${this.result().rowCount()}")
  }
}