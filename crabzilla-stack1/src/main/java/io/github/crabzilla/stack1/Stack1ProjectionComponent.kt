package io.github.crabzilla.stack1

import io.github.crabzilla.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.vertx.core.Handler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Stack1ProjectionComponent(private val stack1: Stack1Component) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(Stack1WebCommandComponent::class.java)
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector) {
    log.info("adding projector for $projectionName fsubscribing on ${stack1.projectionEndpoint}")
    val uolProjector = PgcUowProjector(stack1.readDb, projectionName)
    stack1.vertx.eventBus().consumer<UnitOfWorkEvents>(stack1.projectionEndpoint) { message ->
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
