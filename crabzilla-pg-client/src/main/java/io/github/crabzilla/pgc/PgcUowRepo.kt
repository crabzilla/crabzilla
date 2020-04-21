package io.github.crabzilla.pgc

import io.github.crabzilla.framework.*
import io.github.crabzilla.internal.RangeOfEvents
import io.github.crabzilla.internal.UnitOfWorkEvents
import io.github.crabzilla.internal.UnitOfWorkRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*

internal class PgcUowRepo(private val pgPool: PgPool, private val json: Json) : UnitOfWorkRepository {

  companion object {

    internal val log = LoggerFactory.getLogger(PgcUowRepo::class.java)

    private const val UOW_ID = "uow_id"
    private const val UOW_EVENTS = "uow_events"
    private const val CMD_ID = "cmd_id"
    private const val CMD_DATA = "cmd_data"
    private const val AR_ID = "ar_id"
    private const val AR_NAME = "ar_name"
    private const val VERSION = "version"

    private const val SQL_SELECT_FIELDS = "select uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version"
    const val SQL_SELECT_UOW_BY_CMD_ID = "$SQL_SELECT_FIELDS from units_of_work where cmd_id = $1"
    const val SQL_SELECT_UOW_BY_UOW_ID = "$SQL_SELECT_FIELDS from units_of_work where uow_id = $1"
    const val SQL_SELECT_UOW_BY_ENTITY_ID = "$SQL_SELECT_FIELDS from units_of_work where ar_id = $1 order by version"
    const val SQL_SELECT_AFTER_VERSION =  "select uow_events, version from units_of_work " +
      "where ar_id = $1 and ar_name = $2 and version > $3 order by version"
    private val STREAM_ROWS = 1000

  }

  override fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    val params = Tuple.of(cmdId)
    pgPool.preparedQuery(SQL_SELECT_UOW_BY_CMD_ID)
      .execute(params) { ar ->
      if (ar.failed()) {
        promise.fail(ar.cause())
        return@execute
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        promise.complete(null)
        return@execute
      }
      val row = rows.first()
      val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
      val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
      val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 1)
      val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
      val uowId = row.getLong(UOW_ID)
      val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
        row.getInteger(VERSION)!!, events)
      promise.complete(Pair(uow, uowId))
    }
    return promise.future()
  }

  override fun getUowByUowId(uowId: Long): Future<UnitOfWork> {
    val promise =  Promise.promise<UnitOfWork>()
    val params = Tuple.of(uowId)
    pgPool.preparedQuery(SQL_SELECT_UOW_BY_UOW_ID)
      .execute(params) { ar ->
      if (ar.failed()) {
        promise.fail(ar.cause())
        return@execute
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        promise.complete(null)
        return@execute
      }
      val row = rows.first()
      val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
      val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
      val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 1)
      val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
      val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
          row.getInteger(VERSION)!!, events)
      promise.complete(uow)
    }
    return promise.future()
  }

  override fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    val promise = Promise.promise<List<UnitOfWork>>()
    val params = Tuple.of(id)
    pgPool.preparedQuery(SQL_SELECT_UOW_BY_ENTITY_ID)
      .execute(params) { ar ->
      if (ar.failed()) {
        promise.fail(ar.cause())
        return@execute
      }
      val result = ArrayList<UnitOfWork>()
      val rows = ar.result()
      for (row in rows) {
        val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
        val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
        val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 1)
        val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
        val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
          row.getInteger(VERSION)!!, events)
        result.add(uow)
      }
      promise.complete(result)
    }
    return promise.future()
  }

  override fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String): Future<RangeOfEvents> {
    val promise = Promise.promise<RangeOfEvents>()
    log.trace("will load id [{}] after version [{}]", id, version)
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
          val tuple = Tuple.of( id, aggregateRootName, version)
          val stream = pq.createStream(STREAM_ROWS, tuple)
          val list = ArrayList<RangeOfEvents>()
          // Use the stream
          stream.handler { row ->
            val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 0)
            val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsJson.encode())
            val snapshotData = RangeOfEvents(version, row.getInteger(VERSION)!!, events)
            list.add(snapshotData)
          }
          stream.endHandler {
            log.trace("found {} units of work for id {} and version > {}", list.size, id, version)
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


  override fun selectAfterUowId(uowId: Long, maxRows: Int): Future<List<UnitOfWorkEvents>> {
    val promise = Promise.promise<List<UnitOfWorkEvents>>()
    log.trace("will load after uowId [{}]", uowId)
    val selectAfterUowIdSql = "select uow_id, ar_id, uow_events " +
      "  from units_of_work " +
      " where uow_id > $1 " +
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
