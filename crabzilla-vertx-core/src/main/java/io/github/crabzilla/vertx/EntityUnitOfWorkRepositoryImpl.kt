package io.github.crabzilla.vertx

import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.Version
import io.github.crabzilla.core.listOfEventsFromJson
import io.github.crabzilla.vertx.helpers.VertxSqlHelper.queryWithParams
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import org.slf4j.LoggerFactory.getLogger
import java.util.*

class EntityUnitOfWorkRepositoryImpl(private val client: JDBCClient) : EntityUnitOfWorkRepository {

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

}
