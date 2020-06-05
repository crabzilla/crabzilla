package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.EVENT_SERIALIZER
import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.github.crabzilla.core.command.Version
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.ArrayList

class PgcUowTestRepo(private val pgPool: PgPool, private val json: Json) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcUowTestRepo::class.java)
    private const val UOW_ID = "uow_id"
    private const val AR_ID = "ar_id"
    private const val VERSION = "version"
    private const val SQL_SELECT_AFTER_VERSION = "select uow_events, version from units_of_work " +
      "where ar_id = $1 and ar_name = $2 and version > $3 order by version"
    private const val STREAM_ROWS_PER_PAGE = 1000
  }

  data class RangeOfEvents(val afterVersion: Version, val untilVersion: Version, val events: List<DomainEvent>)

  fun selectAfterVersion(id: Int, version: Version, entityName: String): Future<RangeOfEvents> {
    val promise = Promise.promise<RangeOfEvents>()
    log.debug("will load id [{}] after version [{}]", id, version)
    pgPool.getConnection { ar0 ->
      if (ar0.failed()) {
        promise.fail(ar0.cause())
        return@getConnection
      }
      val conn = ar0.result()
      conn.prepare(SQL_SELECT_AFTER_VERSION) { ar1 ->
        if (ar1.failed()) {
          promise.fail(ar1.cause())
        } else {
          val pq = ar1.result()
          // Fetch STREAM_ROWS rows at a time
          val tuple = Tuple.of(id, entityName, version)
          val stream = pq.createStream(STREAM_ROWS_PER_PAGE, tuple)
          val list = ArrayList<RangeOfEvents>()
          // Use the stream
          stream.handler { row ->
            val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 0)
            val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
            val snapshotData = RangeOfEvents(version, row.getInteger(VERSION)!!, events)
            list.add(snapshotData)
          }
          stream.endHandler {
            log.debug("found {} units of work for id {} and version > {}", list.size, id, version)
            val finalVersion = if (list.size == 0) 0 else list[list.size - 1].untilVersion
            val flatMappedToEvents = list.flatMap { sd -> sd.events }
            promise.complete(RangeOfEvents(version, finalVersion, flatMappedToEvents))
          }
          stream.exceptionHandler { err ->
            log.error(err.message)
            promise.fail(err)
          }
        }
      }
    }
    return promise.future()
  }

  fun selectAfterUowId(uowId: Long, maxRows: Int, entityName: String?): Future<List<UnitOfWorkEvents>> {
    val promise = Promise.promise<List<UnitOfWorkEvents>>()
    log.debug("will load after uowId [{}]", uowId)
    val selectAfterUowIdSql = "select uow_id, ar_id, uow_events " +
      "  from units_of_work " +
      " where uow_id > $1 " +
      if (entityName == null) "" else "and ar_name = '$entityName'" +
      " order by uow_id " +
      " limit " + maxRows
    val list = ArrayList<UnitOfWorkEvents>()
    pgPool.preparedQuery(selectAfterUowIdSql)
      .execute(Tuple.of(uowId)) { ar ->
      if (ar.failed()) {
        promise.fail(ar.cause().message)
        return@execute
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        promise.complete(list)
        return@execute
      }
      for (row in rows) {
        val uowSeq = row.getLong(UOW_ID)
        val targetId = row.getInteger(AR_ID)
        val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 2)
        val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
        val projectionData = UnitOfWorkEvents(uowSeq.toLong(), targetId, events)
        list.add(projectionData)
      }
      promise.complete(list)
    }
    return promise.future()
  }
}
