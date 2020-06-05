package io.github.crabzilla.pgc.query

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcProjectionsRepo(private val readModelDb: PgPool) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcProjectionsRepo::class.java)
    const val SQL_SELECT_LAST_UOW_ID =
      "select max(last_uow) from projections where entityName = $1 and streamId = $2"
  }

  fun selectLastUowId(entityName: String, streamId: String): Future<Long> {
    val promise = Promise.promise<Long>()
    readModelDb
      .preparedQuery(SQL_SELECT_LAST_UOW_ID)
      .execute(Tuple.of(entityName, streamId)) { event ->
        if (event.failed()) {
          promise.fail(event.cause())
          return@execute
        }
        val result = event.result()
        if (result == null || result.size() == 0) {
          promise.fail("Projection fo entity $entityName stream $streamId was not found")
          return@execute
        }
        val finalResult = result.first().getLong(0)
        if (finalResult == null) {
          promise.fail("Projection fo entity $entityName stream $streamId was not found: $finalResult")
          return@execute
        }
        log.info("Result for entity $entityName stream $streamId, final = $finalResult")
        promise.complete(finalResult)
      }
    return promise.future()
  }
}
