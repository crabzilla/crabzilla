package io.github.crabzilla.webpgc

import io.github.crabzilla.Entity
import io.github.crabzilla.EntityCommandAware
import io.github.crabzilla.EntityJsonAware
import io.github.crabzilla.EventBusUowPublisher
import io.github.crabzilla.pgc.PgcCmdHandlerComponent
import io.github.crabzilla.pgc.PgcComponent
import io.github.crabzilla.web.WebEntityComponentImpl
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO should this be a verticle instead?
class WebPgcCmdHandlerComponent(private val pgc: PgcComponent, private val router: Router) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebPgcCmdHandlerComponent::class.java)
  }

  private val eventsPublisher = EventBusUowPublisher(pgc.vertx, pgc.projectionEndpoint)

  fun <E: Entity> addCommandHandler(entityName: String, jsonAware: EntityJsonAware<E>,
                                    cmdAware: EntityCommandAware<E>, resourceName: String) {
    log.info("adding web command handler for entity $entityName on resource $resourceName")
    val cmdHandlerComponent = PgcCmdHandlerComponent(pgc.writeDb, entityName, jsonAware, cmdAware, eventsPublisher)
    WebEntityComponentImpl(cmdHandlerComponent, resourceName, router).deployWebRoutes()
  }

}
