package io.github.crabzilla.vertx

import io.github.crabzilla.*
import io.github.crabzilla.vertx.jdbc.VertxSqlHelper.*
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLRowStream
import io.vertx.ext.sql.UpdateResult
import java.util.*

open class JdbcUnitOfWorkRepository(private val client: JDBCClient) : UnitOfWorkRepository {

  companion object {

    internal var log = org.slf4j.LoggerFactory.getLogger(UnitOfWorkRepository::class.java)

    private val UOW_ID = "uow_id"
    private val UOW_EVENTS = "uow_events"
    private val CMD_DATA = "cmd_data"
    private val VERSION = "version"

    private const val SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id =? "
    private const val SELECT_UOW_BY_UOW_ID = "select * from units_of_work where uow_id =? "

  }

  override fun getUowByCmdId(cmdId: UUID, uowFuture: Future<UnitOfWork>) {
    get(SELECT_UOW_BY_CMD_ID, cmdId, uowFuture)
  }

  override fun getUowByUowId(uowId: UUID, uowFuture: Future<UnitOfWork>) {
    get(SELECT_UOW_BY_UOW_ID, uowId, uowFuture)
  }

  override fun get(query: String, id: UUID, uowFuture: Future<UnitOfWork>) {

    val params = JsonArray().add(id.toString())

    client.getConnection { getConn ->
      if (getConn.failed()) {
        uowFuture.fail(getConn.cause())
        return@getConnection
      }

      val sqlConn = getConn.result()
      val resultSetFuture = Future.future<ResultSet>()
      queryWithParams(sqlConn, query, params, resultSetFuture)

      resultSetFuture.setHandler { resultSetAsyncResult ->
        if (resultSetAsyncResult.failed()) {
          uowFuture.fail(resultSetAsyncResult.cause())
          return@setHandler
        }
        val rs = resultSetAsyncResult.result()
        val rows = rs.rows
        if (rows.size == 0) {
          uowFuture.complete(null)
        } else {
          for (row in rows) { // only 1 will be here
            val command = Json.decodeValue(row.getString(CMD_DATA), Command::class.java)
            val events = listOfEventsFromJson(Json.mapper, row.getString(UOW_EVENTS))
            val uow = UnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
              row.getLong(VERSION)!!, events)
            uowFuture.complete(uow)
          }
        }
        sqlConn.commit {
          sqlConn.close { done ->
            if (done.failed()) {
              log.error("when closing sql connection", done.cause())
            }
          }
        }

      }
    }
  }

  override fun selectAfterVersion(id: String, version: Version,
                                  selectAfterVersionFuture: Future<SnapshotData>,
                                  aggregateRootName: String) {

    log.info("will load id [{}] after version [{}]", id, version)

    val selectAfterVersionSql = "select uow_events, version from units_of_work " +
      " where ar_id = ? " +
      "   and ar_name = ? " +
      "   and version > ? " +
      " order by version "

    val params = JsonArray().add(id).add(aggregateRootName).add(version)

    client.getConnection { getConn ->
      if (getConn.failed()) {
        selectAfterVersionFuture.fail(getConn.cause())
        return@getConnection
      }

      val sqlConn = getConn.result()
      val streamFuture = Future.future<SQLRowStream>()
      queryStreamWithParams(sqlConn, selectAfterVersionSql, params, streamFuture)

      streamFuture.setHandler { ar ->

        if (ar.failed()) {
          selectAfterVersionFuture.fail(ar.cause())
          return@setHandler
        }

        val stream = ar.result()
        val list = ArrayList<SnapshotData>()
        stream
          .resultSetClosedHandler {
            // will ask to restart the stream with the new result set if any
            stream.moreResults()
          }
          .handler { row ->

            val events = listOfEventsFromJson(Json.mapper, row.getString(0))
            val snapshotData = SnapshotData(row.getLong(1)!!, events)
            list.add(snapshotData)
          }.endHandler { event ->

            log.info("found {} units of work for id {} and version > {}",
              list.size, id, version)

            val finalVersion = if (list.size == 0)
              0
            else
              list[list.size - 1].version

            val flatMappedToEvents = list.flatMap { sd -> sd.events }

            selectAfterVersionFuture.complete(SnapshotData(finalVersion, flatMappedToEvents))

            sqlConn.commit {

              sqlConn.close { done ->

                if (done.failed()) {
                  log.error("when closing sql connection", done.cause())
                }

              }
            }

          }

      }

    }
  }

  override fun append(unitOfWork: UnitOfWork, appendFuture: Future<Long>, aggregateRootName: String) {

    val selectCurrentVersionSql = "select max(version) as last_version from units_of_work where ar_id = ? and ar_name = ? "

    val insertUnitOfWorkSql = "insert into units_of_work " +
      "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version) " +
      "values (?, ?, ?, ?, ?, ?, ?)"

    client.getConnection { conn ->

      if (conn.failed()) {
        appendFuture.fail(conn.cause())
        return@getConnection
      }

      val sqlConn = conn.result()
      val startTxFuture = Future.future<Void>()

      // start a transaction
      startTx(sqlConn, startTxFuture)

      startTxFuture.setHandler { startTxAsyncResult ->
        if (startTxAsyncResult.failed()) {
          appendFuture.fail(startTxAsyncResult.cause())
          return@setHandler
        }

        // check current version
        val params1 = JsonArray()
          .add(unitOfWork.targetId().stringValue())
          .add(aggregateRootName)

        val resultSetFuture = Future.future<ResultSet>()
        queryWithParams(sqlConn, selectCurrentVersionSql, params1, resultSetFuture)
        resultSetFuture.setHandler { asyncResultResultSet ->

          if (asyncResultResultSet.failed()) {
            appendFuture.fail(asyncResultResultSet.cause())
            return@setHandler
          }

          val rs = asyncResultResultSet.result()
          var currentVersion = rs.rows[0].getLong("last_version")
          currentVersion = if (currentVersion == null) 0L else currentVersion

          log.info("Found version  {}", currentVersion)

          // apply optimistic locking
          if (currentVersion != unitOfWork.version - 1) {

            val error = DbConcurrencyException(
              String.format("ar_id = [%s], current_version = %d, new_version = %d",
                unitOfWork.targetId().stringValue(),
                currentVersion, unitOfWork.version))

            appendFuture.fail(error)

            // and close the connection
            sqlConn.close { done ->
              if (done.failed()) {
                log.error("when closing sql connection", done.cause())
              }
            }

            return@setHandler
          }

          // if version is OK, then insert
          val cmdAsJson = commandToJson(Json.mapper, unitOfWork.command)
          val eventsListAsJson = listOfEventsToJson(Json.mapper, unitOfWork.events)

          val params2 = JsonArray()
            .add(unitOfWork.unitOfWorkId.toString())
            .add(eventsListAsJson)
            .add(unitOfWork.command.commandId.toString())
            .add(cmdAsJson)
            .add(unitOfWork.targetId().stringValue())
            .add(aggregateRootName)
            .add(unitOfWork.version)

          val updateResultFuture = Future.future<UpdateResult>()
          updateWithParams(sqlConn, insertUnitOfWorkSql, params2, updateResultFuture)

          updateResultFuture.setHandler { asyncResultUpdateResult ->
            if (asyncResultUpdateResult.failed()) {
              appendFuture.fail(asyncResultUpdateResult.cause())
              return@setHandler
            }

            val updateResult = asyncResultUpdateResult.result()
            val commitFuture = Future.future<Void>()

            // commit data
            commitTx(sqlConn, commitFuture)

            commitFuture.setHandler { commitAsyncResult ->

              if (commitAsyncResult.failed()) {
                appendFuture.fail(commitAsyncResult.cause())
                return@setHandler
              }


              appendFuture.complete(updateResult.keys.getLong(0))

              sqlConn.commit {

                // and close the connection
                sqlConn.close { done ->
                  if (done.failed()) {
                    log.error("when closing sql connection", done.cause())
                  }
                }
              }

            }

          }

        }

      }

    }

  }

  override fun selectAfterUowSequence(uowSequence: Long, maxRows: Int,
                                      selectAfterUowSeq: Future<List<ProjectionData>>) {

    log.info("will load after uowSequence [{}]", uowSequence)

    val params = JsonArray().add(uowSequence)

    val selectAfterUowSequenceSql = "select uow_id, uow_seq_number, ar_id as target_id, uow_events " +
      "  from units_of_work " +
      " where uow_seq_number > ? " +
      " order by uow_seq_number " +
      " limit " + maxRows

    client.getConnection { getConn ->
      if (getConn.failed()) {
        selectAfterUowSeq.fail(getConn.cause())
        return@getConnection
      }

      val sqlConn = getConn.result()
      val streamFuture = Future.future<SQLRowStream>()
      queryStreamWithParams(sqlConn, selectAfterUowSequenceSql, params, streamFuture)

      streamFuture.setHandler { ar ->

        if (ar.failed()) {
          selectAfterUowSeq.fail(ar.cause())
          log.error("when scanning for projectionData after uowSequence " + uowSequence, ar.cause())
          return@setHandler
        }

        val stream = ar.result()
        val list = ArrayList<ProjectionData>()
        stream
          .resultSetClosedHandler { _ ->
            // will ask to restart the stream with the new result set if any
            stream.moreResults()
          }
          .handler { row ->
            val uowId = UUID.fromString(row.getString(0))
            val uowSeq = row.getLong(1)
            val targetId = row.getString(2)
            val events = listOfEventsFromJson(Json.mapper, row.getString(3))
            val projectionData = ProjectionData(uowId, uowSeq, targetId, events)
            list.add(projectionData)

          }.endHandler {

            selectAfterUowSeq.complete(list)

            log.info("found {} instances of projectionData after uowSequence {}",
              list.size, uowSequence)

            sqlConn.commit {

              sqlConn.close { done ->

                if (done.failed()) {
                  log.error("when closing sql connection", done.cause())
                }

              }

            }

          }

      }

    }
  }

}




