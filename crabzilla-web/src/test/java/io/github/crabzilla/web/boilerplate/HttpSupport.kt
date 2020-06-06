package io.github.crabzilla.web.boilerplate

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import java.net.ServerSocket
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HttpSupport {

  private val log: Logger = LoggerFactory.getLogger(HttpSupport::class.java)

  fun findFreeHttpPort(): Int {
    var httpPort = 0
    try {
      val socket = ServerSocket(0)
      httpPort = socket.localPort
      socket.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return httpPort
  }

  fun listenHandler(promise: Promise<Void>): Handler<AsyncResult<HttpServer>> {
    return Handler { startedFuture ->
      if (startedFuture.succeeded()) {
        log.info("Server started on port " + startedFuture.result().actualPort())
        promise.complete()
      } else {
        log.error("oops, something went wrong during server initialization", startedFuture.cause())
        promise.fail(startedFuture.cause())
      }
    }
  }
}
