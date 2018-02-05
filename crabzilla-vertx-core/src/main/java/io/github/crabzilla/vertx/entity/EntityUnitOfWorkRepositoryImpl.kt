package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.commandToJson
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.SnapshotData
import io.github.crabzilla.core.entity.Version
import io.github.crabzilla.core.listOfEventsFromJson
import io.github.crabzilla.core.listOfEventsToJson
import io.github.crabzilla.vertx.DbConcurrencyException
import io.github.crabzilla.vertx.helpers.VertxSqlHelper.*
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLRowStream
import io.vertx.ext.sql.UpdateResult
import org.slf4j.LoggerFactory.getLogger
import java.util.*
import java.util.stream.Collectors

class EntityUnitOfWorkRepositoryImpl(private val aggregateRootName: String, private val client: JDBCClient) : EntityUnitOfWorkRepository {

  companion object {

    internal var log = getLogger(EntityUnitOfWorkRepositoryImpl::class.java)

    private val UOW_ID = "uow_id"
    private val UOW_EVENTS = "uow_events"
    private val CMD_DATA = "cmd_data"
    private val VERSION = "version"

    private const val SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id =? "
    private const val SELECT_UOW_BY_UOW_ID = "select * from units_of_work where uow_id =? "

  }

  override fun getUowByCmdId(cmdId: UUID, uowFuture: Future<EntityUnitOfWork>) {

    get(SELECT_UOW_BY_CMD_ID, cmdId, uowFuture)

  }

  override fun getUowByUowId(uowId: UUID, uowFuture: Future<EntityUnitOfWork>) {

    get(SELECT_UOW_BY_UOW_ID, uowId, uowFuture)

  }

  override fun get(querie: String, id: UUID, uowFuture: Future<EntityUnitOfWork>) {

    val params = JsonArray().add(id.toString())

    client.getConnection { getConn ->
      if (getConn.failed()) {
        uowFuture.fail(getConn.cause())
        return@getConnection
      }

      val sqlConn = getConn.result()
      val resultSetFuture = Future.future<ResultSet>()
      queryWithParams(sqlConn, querie, params, resultSetFuture)

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
          for (row in rows) {
            val command = Json.decodeValue(row.getString(CMD_DATA), EntityCommand::class.java)
            val events = listOfEventsFromJson(Json.mapper, row.getString(UOW_EVENTS))
            val uow = EntityUnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
                    Version(row.getLong(VERSION)!!), events)
            uowFuture.complete(uow)
          }
        }
        sqlConn.close { done ->
          if (done.failed()) {
            log.error("when closing sql connection", done.cause())
          }
        }
      }
    }
  }

  override fun selectAfterVersion(id: String, version: Version,
                                  selectAfterVersionFuture: Future<SnapshotData>) {

    log.info("will load id [{}] after version [{}]", id, version.valueAsLong)

    val SELECT_AFTER_VERSION = "select uow_events, version from units_of_work " +
      " where ar_id = ? " +
      "   and ar_name = ? " +
      "   and version > ? " +
      " order by version "

    val params = JsonArray().add(id).add(aggregateRootName).add(version.valueAsLong)

    client.getConnection { getConn ->
      if (getConn.failed()) {
        selectAfterVersionFuture.fail(getConn.cause())
        return@getConnection
      }

      val sqlConn = getConn.result()
      val streamFuture = Future.future<SQLRowStream>()
      queryStreamWithParams(sqlConn, SELECT_AFTER_VERSION, params, streamFuture)

      streamFuture.setHandler { ar ->

        if (ar.failed()) {
          selectAfterVersionFuture.fail(ar.cause())
          return@setHandler
        }

        val stream = ar.result()
        val list = ArrayList<SnapshotData>()
        stream
          .resultSetClosedHandler { v ->
            // will ask to restart the stream with the new result set if any
            stream.moreResults()
          }
          .handler { row ->

            val events = listOfEventsFromJson(Json.mapper, row.getString(0))
            val snapshotData = SnapshotData(Version(row.getLong(1)!!), events)
            list.add(snapshotData)
          }.endHandler { event ->

            log.info("found {} units of work for id {} and version > {}",
              list.size, id, version.valueAsLong)

            val finalVersion = if (list.size == 0)
              Version(0)
            else
              list[list.size - 1].version

            val flatMappedToEvents = list.flatMap { sd -> sd.events }

            selectAfterVersionFuture.complete(SnapshotData(finalVersion, flatMappedToEvents))

            sqlConn.close { done ->

              if (done.failed()) {
                log.error("when closing sql connection", done.cause())
              }

            }

          }

      }

    }
  }

  override fun append(unitOfWork: EntityUnitOfWork, appendFuture: Future<Long>) {

    val SELECT_CURRENT_VERSION = "select max(version) as last_version from units_of_work where ar_id = ? and ar_name = ? "

    val INSERT_UOW = "insert into units_of_work " +
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
        queryWithParams(sqlConn, SELECT_CURRENT_VERSION, params1, resultSetFuture)
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
          if (currentVersion != unitOfWork.version.valueAsLong - 1) {

            val error = DbConcurrencyException(
              String.format("ar_id = [%s], current_version = %d, new_version = %d",
                unitOfWork.targetId().stringValue(),
                currentVersion, unitOfWork.version.valueAsLong))

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
            .add(unitOfWork.version.valueAsLong)

          val updateResultFuture = Future.future<UpdateResult>()
          updateWithParams(sqlConn, INSERT_UOW, params2, updateResultFuture)

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
