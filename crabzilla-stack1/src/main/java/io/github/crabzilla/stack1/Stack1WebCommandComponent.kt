package io.github.crabzilla.stack1

import io.github.crabzilla.Entity
import io.github.crabzilla.EntityCommandAware
import io.github.crabzilla.EntityJsonAware
import io.github.crabzilla.EventBusUowPublisher
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.web.WebEntityComponentImpl
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Stack1WebCommandComponent(private val stack1: Stack1Component,
                                private val router: Router) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(Stack1WebCommandComponent::class.java)
  }
  fun <E: Entity> addEntity(entityName: String,
                            jsonAware: EntityJsonAware<E>,
                            cmdAware: EntityCommandAware<E>, resourceName: String) {
    log.info("adding entity $entityName")
    val entityComponent = PgcEntityComponent(
      stack1.writeDb, entityName, jsonAware, cmdAware, EventBusUowPublisher(stack1.vertx, stack1.projectionEndpoint))
    WebEntityComponentImpl(entityComponent, resourceName, router).deployWebRoutes()
  }
}
