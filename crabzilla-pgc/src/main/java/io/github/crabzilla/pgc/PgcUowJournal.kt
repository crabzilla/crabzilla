package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.slf4j.LoggerFactory

class PgcUowJournal(private val pgPool: PgPool,
                    private val cmdToJson: (Command) -> String,
                    private val eventToJson: (DomainEvent) -> String) : UnitOfWorkJournal {

  companion object {

    internal val log = LoggerFactory.getLogger(PgcUowJournal::class.java)

    const val SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version " +
                                       "from units_of_work where ar_id = $1 and ar_name = $2 "

    const val SQL_APPEND_UOW = "insert into units_of_work " +
                                        "(uow_id, uow_events, cmd_id, cmd_name, cmd_data, ar_name, ar_id, version) " +
                                        "values ($1, $2, $3, $4, $5, $6, $7, $8) returning uow_seq_number"
  }

  override fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Int>>) {

    pgPool.getConnection { conn ->

      if (conn.failed()) {
        aHandler.handle(Future.failedFuture(conn.cause())); return@getConnection
      }

      val sqlConn = conn.result()

      // Begin the transaction
      val tx = sqlConn
        .begin()
        .abortHandler { run { log.error("Transaction failed => rollback") }
        }

      val params = Tuple.of(unitOfWork.targetId, unitOfWork.targetName)

      sqlConn.preparedQuery(SQL_SELECT_CURRENT_VERSION, params) { ar ->

        if (ar.failed()) {
          aHandler.handle(Future.failedFuture(ar.cause()))

        } else {

          val currentVersion = ar.result().first()?.getInteger("last_version")?: 0

          log.info("Found version  {}", currentVersion)

          // version does not match
          if (currentVersion != unitOfWork.version -1) {
            val error =
              DbConcurrencyException("expected version is ${unitOfWork.version} but current version is $currentVersion")
            aHandler.handle(Future.failedFuture(error))

          } else {

            // if version is OK, then insert
            val cmdAsJson = cmdToJson.invoke(unitOfWork.command)

            val eventsListAsJson = unitOfWork.events.toJsonArray(eventToJson).encode()

            val params2 = Tuple.of(
              unitOfWork.unitOfWorkId,
              io.reactiverse.pgclient.data.Json.create(eventsListAsJson),
              unitOfWork.commandId,
              unitOfWork.commandName,
              io.reactiverse.pgclient.data.Json.create(cmdAsJson),
              unitOfWork.targetName,
              unitOfWork.targetId,
              unitOfWork.version)

            sqlConn.preparedQuery(SQL_APPEND_UOW, params2) { insert ->

              if (insert.failed()) {
                log.error("SQL_APPEND_UOW", insert.cause())
                aHandler.handle(Future.failedFuture(insert.cause()))

              } else {
                val insertRows = insert.result().value()
                val generated = insertRows.first().getInteger(0)

                // Commit the transaction
                tx.commit { ar ->
                  if (ar.failed()) {
                    log.error("Transaction failed " + ar.cause().message)
                    aHandler.handle(Future.failedFuture(ar.cause()))
                  } else {
                    log.info("Transaction succeeded")
                    aHandler.handle(Future.succeededFuture(generated))
                  }
                }
              }

            }

          }

        }

      }

    }

  }

}
