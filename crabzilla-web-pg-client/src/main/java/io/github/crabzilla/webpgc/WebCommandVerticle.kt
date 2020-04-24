package io.github.crabzilla.webpgc

import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class WebCommandVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebCommandVerticle::class.java)
  }

  val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }
  val httpPort: Int by lazy { config().getInteger("WRITE_HTTP_PORT") }

  fun <E : Entity> addResourceForEntity(
    resourceName: String,
    json: Json,
    entityName: String,
    cmdAware: EntityCommandAware<E>,
    router: Router
  ) {
    log.info("adding web command handler for entity $entityName on resource $resourceName")
    val cmdHandlerComponent = PgcEntityComponent(vertx, writeDb, json, entityName, cmdAware)
    WebDeployer(cmdHandlerComponent, resourceName, router).deployWebRoutes()
  }
}
