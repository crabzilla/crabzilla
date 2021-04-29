package io.github.crabzilla.pgc

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import org.slf4j.LoggerFactory

object PgcClient {

  private val log = LoggerFactory.getLogger(PgcClient::class.java)

  fun rollback(tx: Transaction, throwable: Throwable) {
    log.error("Will rollback transaction given ${throwable.message}")
    tx.rollback()
      .onFailure { log.error("On transaction rollback", it) }
      .onSuccess {
        log.debug("Transaction successfully rolled back")
      }
  }

  fun close(conn: SqlConnection) {
    log.debug("Will close db connection")
    conn.close()
      .onFailure { log.error("When closing db connection") }
      .onSuccess { log.debug("Connection closed") }
  }

  fun commit(tx: Transaction): Future<Void> {
    val promise = Promise.promise<Void>()
    log.debug("Will commit transaction")
    tx.commit()
      .onFailure {
        log.error("When committing the transaction", it)
        promise.fail(it.cause)
      }
      .onSuccess {
        log.debug("Transaction committed")
        promise.complete()
      }
    return promise.future()
  }
}
