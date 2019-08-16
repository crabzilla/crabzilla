package io.github.crabzilla.pgc

import io.github.crabzilla.UnitOfWorkEvents
import io.vertx.core.Handler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

class PgcProjectionComponent(private val pgc: PgcComponent) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(PgcProjectionComponent::class.java)
    private val processId = ManagementFactory.getRuntimeMXBean().name
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector) {
    log.info("adding projector for $projectionName subscribing on ${pgc.projectionEndpoint}")
    val uolProjector = PgcUowProjector(pgc.readDb, projectionName)
    pgc.vertx.eventBus().consumer<UnitOfWorkEvents>(pgc.projectionEndpoint) { message ->
      uolProjector.handle(message.body(), projector, Handler { result ->
        if (result.failed()) { // TODO circuit breaker
          log.error("Projection [$projectionName] failed: " + result.cause().message)
        } else {
          log.info("Projection success")
        }
      })
    }
    pgc.vertx.eventBus().consumer<String>(whoIsRunningProjection(pgc.projectionEndpoint)) { msg ->
      log.info("received " + msg.body())
      msg.reply("Yes, I'm running here: $processId")
    }
  }

}
