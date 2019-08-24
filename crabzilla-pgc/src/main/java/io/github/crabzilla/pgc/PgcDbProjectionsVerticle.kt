package io.github.crabzilla.pgc

import io.github.crabzilla.UnitOfWorkEvents
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

abstract class PgcDbProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(PgcDbProjectionsVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
    fun amIAlreadyRunning(projectionEndpoint: String) : String {
      return "$projectionEndpoint-ping"
    }
  }

  private val projectionEndpoint: String by lazy {
    config().getString("PROJECTION_ENDPOINT")
  }

  private val readDb : PgPool by lazy {
    readModelPgPool(vertx, config())
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<String>(amIAlreadyRunning(projectionEndpoint)) { msg ->
      log.info("received " + msg.body())
      msg.reply("Yes, $projectionEndpoint is already running here: $processId")
    }
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector) {
    log.info("adding projector for $projectionName subscribing on $projectionEndpoint")
    val uolProjector = PgcUowProjector(readDb, projectionName)
    vertx.eventBus().consumer<UnitOfWorkEvents>(projectionEndpoint) { message ->
      uolProjector.handle(message.body(), projector, Handler { result ->
        if (result.failed()) { // TODO circuit breaker
          log.error("Projection [$projectionName] failed: " + result.cause().message)
        } else {
          log.info("Projection success")
        }
      })
    }
  }

}
