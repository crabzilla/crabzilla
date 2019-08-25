package io.github.crabzilla.webpgc

import io.github.crabzilla.*
import io.github.crabzilla.pgc.PgcCmdHandler
import io.github.crabzilla.pgc.readModelPgPool
import io.github.crabzilla.pgc.writeModelPgPool
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class WebCmdHandlerVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebCmdHandlerVerticle::class.java)
  }

  val projectionEndpoint: String by lazy {
    config().getString("PROJECTION_ENDPOINT")
  }
  val eventsPublisher: UnitOfWorkPublisher by lazy {
    EventBusUowPublisher(vertx, projectionEndpoint)
  }
  val readDb : PgPool by lazy {
    readModelPgPool(vertx, config())
  }
  val writeDb : PgPool by lazy {
    writeModelPgPool(vertx, config())
  }

  fun <E: Entity> addResourceForEntity(resourceName: String, entityName: String,
                                       jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>,
                                       router: Router) {
    log.info("adding web command handler for entity $entityName on resource $resourceName")
    val cmdHandlerComponent = PgcCmdHandler(writeDb, entityName, jsonAware, cmdAware, eventsPublisher)
    WebDeployer(cmdHandlerComponent, resourceName, router).deployWebRoutes()
  }

}
