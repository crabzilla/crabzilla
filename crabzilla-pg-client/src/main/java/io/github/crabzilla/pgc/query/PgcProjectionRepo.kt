package io.github.crabzilla.pgc.query

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool

class PgcProjectionRepo(val readDb: PgPool) {

  fun selectLastUowId(entityName: String, streamId: String): Future<Long> {
    val promise = Promise.promise<Long>()
    val selectLastUowIdSql =
      "select max(last_uow) from projections where entityName = '$entityName' and streamId = '$streamId'"
    readDb
      .preparedQuery(selectLastUowIdSql)
      .execute { event ->
        if (event.failed()) {
          promise.fail(event.cause())
          return@execute
        }
        val result = event.result()
        promise.complete(if (result == null || result.size() == 0) 0 else result.first().getLong(0))
      }
    return promise.future()
  }
}
