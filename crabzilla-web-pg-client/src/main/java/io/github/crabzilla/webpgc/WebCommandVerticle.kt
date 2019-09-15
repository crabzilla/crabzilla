package io.github.crabzilla.webpgc

import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityCommandAware
import io.github.crabzilla.framework.EntityJsonAware
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class WebCommandVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebCommandVerticle::class.java)
  }

  val writeDb : PgPool by lazy { writeModelPgPool(vertx, config()) }
  val jsonFunctions: MutableMap<String, EntityJsonAware<out Entity>> = mutableMapOf()
  val httpPort : Int by lazy { config().getInteger("WRITE_HTTP_PORT")}

  fun <E: Entity> addResourceForEntity(resourceName: String,
                                       entityName: String,
                                       jsonAware: EntityJsonAware<E>,
                                       cmdAware: EntityCommandAware<E>,
                                       router: Router) {
    log.info("adding web command handler for entity $entityName on resource $resourceName")
    jsonFunctions[entityName] = jsonAware
    val cmdHandlerComponent = PgcEntityComponent(vertx, writeDb, entityName, jsonAware, cmdAware)
    WebDeployer(cmdHandlerComponent, resourceName, router).deployWebRoutes()
  }

}
