package io.github.crabzilla.pgc

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import org.slf4j.LoggerFactory

object PgcClient {

  private val log = LoggerFactory.getLogger(PgcClient::class.java)

  fun rollback(tx: Transaction, throwable: Throwable) {
    log.error("Will rollback transaction given", throwable)
    tx.rollback()
      .onFailure { log.error("On transaction rollback", it.cause) }
      .onSuccess {
        log.info("Transaction successfully rolled back")
      }
  }

  fun close(conn: SqlConnection) {
    log.info("Will close db connection")
    conn.close()
      .onFailure { log.error("When closing db connection") }
      .onSuccess { log.info("Connection closed") }
  }

  fun commit(tx: Transaction): Future<Void> {
    val promise = Promise.promise<Void>()
    log.info("Will commit transaction")
    tx.commit()
      .onFailure {
        log.error("When committing the transaction", it.cause)
        promise.fail(it.cause)
      }
      .onSuccess {
        log.info("Transaction committed")
        promise.complete()
      }
    return promise.future()
  }

}
