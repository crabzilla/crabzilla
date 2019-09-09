package io.github.crabzilla.pgc

import io.github.crabzilla.Entity
import io.github.crabzilla.EntityJsonAware
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.internal.UnitOfWorkJournal
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcUowJournal<E: Entity>(private val pgPool: PgPool,
                               private val jsonFunctions: EntityJsonAware<E>) : UnitOfWorkJournal<E> {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcUowJournal::class.java)
    const val SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version " +
                                       "from units_of_work where ar_id = $1 and ar_name = $2 "
    const val SQL_APPEND_UOW = "insert into units_of_work " +
                                        "(uow_events, cmd_id, cmd_name, cmd_data, ar_name, ar_id, version) " +
                                        "values ($1, $2, $3, $4, $5, $6, $7) returning uow_id"
  }

  override fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Long>>) {

    pgPool.begin { res ->
      if (res.succeeded()) {
        val tx = res.result()
        val params1 = Tuple.of(unitOfWork.entityId, unitOfWork.entityName)
        tx.preparedQuery(SQL_SELECT_CURRENT_VERSION, params1) { event1 ->
          if (event1.succeeded()) {
            val currentVersion = event1.result().first()?.getInteger("last_version") ?: 0
            if (currentVersion == unitOfWork.version - 1) {
              // if version is OK, then insert
              val cmdAsJsonObject = jsonFunctions.cmdToJson(unitOfWork.command)
              val eventsListAsJsonArray = jsonFunctions.toJsonArray(unitOfWork.events)
              val params2 = Tuple.of(
                eventsListAsJsonArray,
                unitOfWork.commandId,
                unitOfWork.commandName,
                cmdAsJsonObject,
                unitOfWork.entityName,
                unitOfWork.entityId,
                unitOfWork.version)
              tx.preparedQuery(SQL_APPEND_UOW, params2) { event2 ->
                if (event2.succeeded()) {
                  val insertRows = event2.result().value()
                  val generated = insertRows.first().getLong(0)
                  // Commit the transaction
                  tx.commit { event3 ->
                    if (event3.failed()) {
                      log.error("Transaction failed", event3.cause())
                      aHandler.handle(Future.failedFuture(event3.cause()))
                    } else {
                      log.trace("Transaction succeeded for $generated")
                      aHandler.handle(Future.succeededFuture(generated))
                    }
                  }
                } else {
                  log.error("Transaction failed", event2.cause())
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
