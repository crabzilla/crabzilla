package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class PgcUowJournal(private val pgPool: PgPool,
                    private val cmdToJson: (Command) -> JsonObject,
                    private val eventToJson: (DomainEvent) -> JsonObject) : UnitOfWorkJournal {

  companion object {

    internal val log = LoggerFactory.getLogger(PgcUowJournal::class.java)

    const val SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version " +
                                       "from units_of_work where ar_id = $1 and ar_name = $2 "

    const val SQL_APPEND_UOW = "insert into units_of_work " +
                                        "(uow_id, uow_events, cmd_id, cmd_name, cmd_data, ar_name, ar_id, version) " +
                                        "values ($1, $2, $3, $4, $5, $6, $7, $8) returning uow_seq_number"
  }

  override fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Int>>) {

    pgPool.begin { res ->
      if (res.succeeded()) {
        val tx = res.result()
        val params1 = Tuple.of(unitOfWork.entityId, unitOfWork.entityName)
        tx.preparedQuery(SQL_SELECT_CURRENT_VERSION, params1) { event1 ->
          if (event1.succeeded()) {
            val currentVersion = event1.result().first()?.getInteger("last_version") ?: 0
            if (currentVersion == unitOfWork.version - 1) {
              // if version is OK, then insert
              val cmdAsJsonObject = cmdToJson.invoke(unitOfWork.command)
              val eventsListAsJsonObject = unitOfWork.events.toJsonArray(eventToJson)
              val params2 = Tuple.of(
                unitOfWork.unitOfWorkId,
                io.reactiverse.pgclient.data.Json.create(eventsListAsJsonObject),
                unitOfWork.commandId,
                unitOfWork.commandName,
                io.reactiverse.pgclient.data.Json.create(cmdAsJsonObject),
                unitOfWork.entityName,
                unitOfWork.entityId,
                unitOfWork.version)
              tx.preparedQuery(SQL_APPEND_UOW, params2) { event2 ->
                if (event2.succeeded()) {
                  val insertRows = event2.result().value()
                  val generated = insertRows.first().getInteger(0)
                  // Commit the transaction
                  tx.commit { event3 ->
                    if (event3.failed()) {
                      log.error("Transaction failed", event3.cause());
                      aHandler.handle(Future.failedFuture(event3.cause()))
                    } else {
                      log.info("Transaction succeeded for ${unitOfWork.unitOfWorkId}")
                      aHandler.handle(Future.succeededFuture(generated))
                    }
                  }
                } else {
                  log.error("Transaction failed", event2.cause());
                  aHandler.handle(Future.failedFuture(event2.cause()))
                }
              }
            } else {
              val error = "expected version is ${unitOfWork.version - 1} but current version is $currentVersion"
              log.error(error)
              aHandler.handle(Future.failedFuture(error))
            }
          } else {
            log.error("when selecting current version")
            aHandler.handle(Future.failedFuture(event1.cause()))
          }
        }
      } else {
        log.error("when starting transaction")
        aHandler.handle(Future.failedFuture(res.cause()))
      }
    }
  }

}
