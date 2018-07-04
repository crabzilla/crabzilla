package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.*
import io.github.crabzilla.vertx.DbConcurrencyException
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory.getLogger
import java.util.*

open class PgClientUowRepo(private val pgPool: PgPool) : UnitOfWorkRepository {

  companion object {

    internal val log = getLogger(UnitOfWorkRepository::class.java)

    private const val UOW_ID = "uow_id"
    private const val UOW_EVENTS = "uow_events"
    private const val CMD_DATA = "cmd_data"
    private const val VERSION = "version"

    const val SQL_SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id = $1 "
    const val SQL_SELECT_UOW_BY_UOW_ID = "select * from units_of_work where uow_id = $1 "
    const val SQL_SELECT_AFTER_VERSION = "select uow_events, version from units_of_work " +
                                          "where ar_id = $1 and ar_name = $2 and version > $3 order by version "
    const val SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version " +
                                       "from units_of_work where ar_id = $1 and ar_name = $2 "
    const val SQL_INSERT_UOW = "insert into units_of_work " +
                                            "(uow_id, uow_events, cmd_id, cmd_data, ar_name, ar_id, version) " +
                                            "values ($1, $2, $3, $4, $5, $6, $7) returning uow_seq_number"
  }

  override fun getUowByCmdId(cmdId: UUID, uowFuture: Future<UnitOfWork>) {
    get(SQL_SELECT_UOW_BY_CMD_ID, cmdId, uowFuture)
  }

  override fun getUowByUowId(uowId: UUID, uowFuture: Future<UnitOfWork>) {
    get(SQL_SELECT_UOW_BY_UOW_ID, uowId, uowFuture)
  }

  override fun get(query: String, id: UUID, uowFuture: Future<UnitOfWork>) {
    val params = Tuple.of(id)
    pgPool.preparedQuery(query, params) { ar ->
      if (ar.failed()) {
        log.error("get", ar.cause())
        uowFuture.fail(ar.cause())
        return@preparedQuery
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        uowFuture.complete(null)
        return@preparedQuery
      }
      val row = rows.first()
      val command = commandFromJson(Json.mapper, row.getJson(CMD_DATA).value().toString())
      val events = listOfEventsFromJson(Json.mapper, row.getJson(UOW_EVENTS).value().toString())
      val uow = UnitOfWork(row.getUUID(UOW_ID), command, row.getInteger(VERSION)!!, events)
      uowFuture.complete(uow)
    }
  }

  override fun selectAfterVersion(id: Int, version: Version,
                                  selectAfterVersionFuture: Future<SnapshotData>,
                                  aggregateRootName: String) {
    log.info("will load id [{}] after version [{}]", id, version)
    pgPool.getConnection({ ar0 ->
      if (ar0.failed()) {
        log.error("SQL_SELECT_AFTER_VERSION", ar0.cause())
        selectAfterVersionFuture.fail(ar0.cause())
        return@getConnection
      }
      val conn = ar0.result()
      conn.prepare(SQL_SELECT_AFTER_VERSION, { ar1 ->
        if (ar1.succeeded()) {
          val pq = ar1.result()
          // Fetch 100 rows at a time
          val tuple = Tuple.of( id, aggregateRootName, version)
          val stream = pq.createStream(100, tuple)
          val list = ArrayList<SnapshotData>() // TODO what is the point of this list?
          // Use the stream
          stream.exceptionHandler({ err -> log.error("Error: " + err.message, err.cause) })
          stream.endHandler({ _ ->
            log.info("found {} units of work for id {} and version > {}", list.size, id, version)
            val finalVersion = if (list.size == 0) 0 else list[list.size - 1].version
            val flatMappedToEvents = list.flatMap { sd -> sd.events }
            selectAfterVersionFuture.complete(SnapshotData(finalVersion, flatMappedToEvents))
          })
          stream.handler({ row ->
            val events = listOfEventsFromJson(Json.mapper, row.getJson(0).value().toString())
            val snapshotData = SnapshotData(row.getInteger(1)!!, events)
            list.add(snapshotData)
          })
        }
      })
    })
  }


  override fun selectAfterUowSequence(uowSequence: Long, maxRows: Int,
                                      selectAfterUowSeq: Future<List<ProjectionData>>) {

    log.info("will load after uowSequence [{}]", uowSequence)

    val selectAfterUowSequenceSql = "select uow_id, uow_seq_number, ar_id as target_id, uow_events " +
      "  from units_of_work " +
      " where uow_seq_number > $1 " +
      " order by uow_seq_number " +
      " limit " + maxRows

    val list = ArrayList<ProjectionData>()

    pgPool.preparedQuery(selectAfterUowSequenceSql, Tuple.of(uowSequence.toInt())) { ar ->
      if (ar.failed()) {
        log.error("selectAfterUowSequenceSql", ar.cause())
        selectAfterUowSeq.fail(ar.cause())
        return@preparedQuery
      }
      val rows = ar.result()
      if (rows.size() == 0) {
        selectAfterUowSeq.complete(list)
        return@preparedQuery
      }
      for (row in rows) {
        val uowId = row.getUUID(0)
        val uowSeq = row.getLong(1)
        val targetId = row.getInteger(2)
        val events = listOfEventsFromJson(Json.mapper, row.getJson(3).toString())
        val projectionData = ProjectionData(uowId, uowSeq, targetId, events)
        list.add(projectionData)
      }
      selectAfterUowSeq.complete(list)
    }

  }

  override fun append(unitOfWork: UnitOfWork, appendFuture: Future<Long>, aggregateRootName: String) {

    pgPool.getConnection { conn ->

      if (conn.failed()) {
        appendFuture.fail(conn.cause())
        return@getConnection
      }

      val sqlConn = conn.result()

      // Begin the transaction
      val tx = sqlConn
        .begin()
        .abortHandler({ _ ->
          run {
            log.error("Transaction failed =  > rollbacked")
          }
        })

      val params = Tuple.of(unitOfWork.targetId().value(), aggregateRootName)

      sqlConn.preparedQuery(SQL_SELECT_CURRENT_VERSION, params) { ar ->

        if (ar.failed()) {
          log.error("SQL_SELECT_CURRENT_VERSION", ar.cause())
          appendFuture.fail(ar.cause())
          return@preparedQuery
        }

        val currentVersion = ar.result().first()?.getInteger("last_version")?: 0

        log.info("Found version  {}", currentVersion)

        // version does not match
        if (currentVersion != unitOfWork.version -1) {
          val error = DbConcurrencyException("ar_id = [${unitOfWork.targetId().value()}], " +
            "current_version = $currentVersion, new_version = ${unitOfWork.version}")
          appendFuture.fail(error)
          return@preparedQuery
        }

        // if version is OK, then insert
        val cmdAsJson = io.reactiverse.pgclient.Json.create(commandToJson(Json.mapper, unitOfWork.command))
        val eventsListAsJson = io.reactiverse.pgclient.Json.create(listOfEventsToJson(Json.mapper, unitOfWork.events))

        val params2 = Tuple.of(
          unitOfWork.unitOfWorkId,
          eventsListAsJson,
          unitOfWork.command.commandId,
          cmdAsJson,
          aggregateRootName,
          unitOfWork.targetId().value(),
          unitOfWork.version)

        sqlConn.preparedQuery(SQL_INSERT_UOW, params2) { insert ->

          if (insert.failed()) {
            log.error("SQL_INSERT_UOW", insert.cause())
            appendFuture.fail(insert.cause())
            return@preparedQuery
          }

          val insertRows = insert.result().value()
          val generated = insertRows.first().getLong(0)

          // Commit the transaction
          tx.commit { ar ->
            if (ar.succeeded()) {
              log.info("Transaction succeeded")
              appendFuture.complete(generated)
            } else {
              log.error("Transaction failed " + ar.cause().message)
              appendFuture.fail(ar.cause().message)
            }
          }

        }

      }

    }

  }

}
