package io.github.crabzilla.stack1

import io.github.crabzilla.*
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.readModelPgPool
import io.github.crabzilla.pgc.writeModelPgPool
import io.github.crabzilla.web.WebEntityComponentImpl
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Stack1WebApp(private val vertx: Vertx, private val router: Router, config: JsonObject,
                   val projectionEndpoint: String) {

  val readDb = readModelPgPool(vertx, config)
  val writeDb = writeModelPgPool(vertx, config)
  private val entities = mutableMapOf<String, EntityComponent<out Entity>>()

  companion object {
    val log: Logger = LoggerFactory.getLogger(Stack1WebApp::class.java)
  }

  fun <E: Entity> addEntity(name: String, jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>) {
    log.info("adding entity $name")
    entities[name] =
      PgcEntityComponent(writeDb, name, jsonAware, cmdAware, EventBusUowPublisher(vertx, projectionEndpoint))
  }

  fun addWebResource(resourceName: String, entityName: String) {
     log.info("adding resource $resourceName for entity $entityName")
     require(entities.containsKey(entityName)) {"entity $entityName must be deployed before resource $resourceName"}
     WebEntityComponentImpl(entities[entityName]!!, resourceName, router).deployWebRoutes()
  }

}
