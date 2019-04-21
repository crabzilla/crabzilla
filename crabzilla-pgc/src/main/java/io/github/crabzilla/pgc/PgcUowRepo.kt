package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.JsonMetadata.EVENTS_JSON_CONTENT
import io.github.crabzilla.JsonMetadata.EVENT_NAME
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*

open class PgcUowRepo(private val pgPool: PgPool,
                      private val cmdFromJson: (String, JsonObject) -> Command,
                      private val eventFromJson: (String, JsonObject) -> DomainEvent) : UnitOfWorkRepository {

  companion object {

    internal val log = LoggerFactory.getLogger(PgcUowRepo::class.java)

    private const val UOW_ID = "uow_id"
    private const val UOW_EVENTS = "uow_events"
    private const val CMD_ID = "cmd_id"
    private const val CMD_DATA = "cmd_data"
    private const val CMD_NAME = "cmd_name"
    private const val TARGET_ID = "ar_id"
    private const val TARGET_NAME = "ar_name"
    private const val VERSION = "version"

    const val SQL_SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id = $1 "
    const val SQL_SELECT_UOW_BY_UOW_ID = "select $UOW_ID,$UOW_EVENTS,$CMD_ID,$CMD_DATA,$CMD_NAME,$TARGET_ID," +
                                              "$TARGET_NAME, $VERSION from units_of_work where uow_id = $1 "
    const val SQL_SELECT_AFTER_VERSION =  "select $UOW_EVENTS,$VERSION from units_of_work " +
                                          "where ar_id = $1 and ar_name = $2 and version > $3 order by version "
  }

  override fun getUowByCmdId(cmdId: UUID, future: Future<UnitOfWork>) {
    get(SQL_SELECT_UOW_BY_CMD_ID, cmdId, future)
  }

  override fun getUowByUowId(uowId: UUID, future: Future<UnitOfWork>) {
    get(SQL_SELECT_UOW_BY_UOW_ID, uowId, future)
  }

  override fun get(query: String, id: UUID, future: Future<UnitOfWork>) {
    val params = Tuple.of(id)

    pgPool.preparedQuery(query, params) { ar ->
      if (ar.failed()) {
        future.fail(ar.cause()); return@preparedQuery
      }

      val rows = ar.result()
      if (rows.size() == 0) {
        future.complete(null); return@preparedQuery
      }
      val row = rows.first()
      val commandName = row.getString(CMD_NAME)
      val commandAsJson = row.getJson(CMD_DATA).value().toString()
      val command = try { cmdFromJson.invoke(commandName, JsonObject(commandAsJson)) } catch (e: Exception) { null }

      if (command == null) {
        future.fail("error when getting command $commandName from json "); return@preparedQuery
      }

      val jsonArray = JsonArray(row.getJson(UOW_EVENTS).value().toString())

      val jsonToEventPair: (Int) -> DomainEvent = { index ->
        val jsonObject = jsonArray.getJsonObject(index)
        val eventName = jsonObject.getString(EVENT_NAME)
        val eventJson = jsonObject.getJsonObject(EVENTS_JSON_CONTENT)
        val domainEvent = eventFromJson.invoke(eventName, eventJson)
        domainEvent
      }
      try {
        val events: List<DomainEvent> = List(jsonArray.size(), jsonToEventPair)
        val uow = UnitOfWork(row.getUUID(UOW_ID), row.getString(TARGET_NAME), row.getInteger(TARGET_ID),
          row.getUUID(CMD_ID), row.getString(CMD_NAME), command, row.getInteger(VERSION)!!, events)
        future.complete(uow)
      } catch (e:  Exception) {
        future.fail(e)
      }
    }
  }

  override fun selectAfterVersion(id: Int, version: Version,
                                  aggregateRootName: String,
                                  aHandler: Handler<AsyncResult<SnapshotData>>
                                  ) {
    log.info("will load id [{}] after version [{}]", id, version)
    pgPool.getConnection { ar0 ->
      if (ar0.failed()) {
        log.error("get connection", ar0.cause())
        aHandler.handle(Future.failedFuture(ar0.cause()))
        return@getConnection
      }
      val conn = ar0.result()
      conn.prepare(SQL_SELECT_AFTER_VERSION) { ar1 ->

        if (ar1.failed()) {
          aHandler.handle(Future.failedFuture(ar1.cause()))

        } else {
          val pq = ar1.result()
          // Fetch 100 rows at a time
          val tuple = Tuple.of( id, aggregateRootName, version)
          val stream = pq.createStream(100, tuple)
          val list = ArrayList<SnapshotData>()
          // Use the stream
          stream.handler { row ->
            val eventsArray = JsonArray(row.getJson(UOW_EVENTS).value().toString())
            val jsonToEventPair: (Int) -> DomainEvent = { index ->
              val jsonObject = eventsArray.getJsonObject(index)
              val eventName = jsonObject.getString(EVENT_NAME)
              val eventJson = jsonObject.getJsonObject(EVENTS_JSON_CONTENT)
              eventFromJson.invoke(eventName, eventJson)
            }

            val events: List<DomainEvent>? =
              try { List(eventsArray.size(), jsonToEventPair)} catch (e: Exception) { null }

            if (events == null) {
              throw IllegalStateException("when instantiating event from json")
            }

            val snapshotData = SnapshotData(row.getInteger(1)!!, events)
            list.add(snapshotData)
          }
          stream.endHandler {
            log.info("found {} units of work for id {} and version > {}", list.size, id, version)
            val finalVersion = if (list.size == 0) 0 else list[list.size - 1].version
            val flatMappedToEvents = list.flatMap { sd -> sd.events }
            aHandler.handle(Future.succeededFuture(SnapshotData(finalVersion, flatMappedToEvents)))
          }
          stream.exceptionHandler { err ->
            log.error("SQL_SELECT_AFTER_VERSION: " + err.message)
            aHandler.handle(Future.failedFuture(err))
          }
        }
      }
    }
  }


  override fun selectAfterUowSequence(uowSequence: Int, maxRows: Int,
                                      future: Future<List<ProjectionData>>) {

    log.info("will load after uowSequence [{}]", uowSequence)

    val selectAfterUowSequenceSql = "select uow_id, uow_seq_number, ar_id as target_id, uow_events " +
      "  from units_of_work " +
      " where uow_seq_number > $1 " +
      " order by uow_seq_number " +
      " limit " + maxRows

    val list = ArrayList<ProjectionData>()

    pgPool.preparedQuery(selectAfterUowSequenceSql, Tuple.of(uowSequence)) { ar ->
      if (ar.failed()) {
        log.error("selectAfterUowSequenceSql", ar.cause())
        future.fail(ar.cause())
        return@preparedQuery
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        future.complete(list)
        return@preparedQuery
      }
      for (row in rows) {
        val uowId = row.getUUID(0)
        val uowSeq = row.getInteger(1)
        val targetId = row.getInteger(2)
        val eventsArray = JsonArray(row.getJson(UOW_EVENTS).value().toString())
        val jsonToEventPair: (Int) -> DomainEvent = { index ->
          val jsonObject = eventsArray.getJsonObject(index)
          val eventName = jsonObject.getString(EVENT_NAME)
          val eventJson = jsonObject.getJsonObject(EVENTS_JSON_CONTENT)
          eventFromJson.invoke(eventName, eventJson)
        }
        val events: List<DomainEvent> = List(eventsArray.size(), jsonToEventPair)
        val projectionData = ProjectionData(uowId, uowSeq, targetId, events)
        list.add(projectionData)
      }
      future.complete(list)
    }

  }

}
