package io.github.crabzilla.stack1

import io.github.crabzilla.*
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.pgPool
import io.github.crabzilla.web.WebEntityComponentImpl
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CrabzillaApplication(private val vertx: Vertx, private val router: Router, config: JsonObject,
                           private val projectionEndpoint: String) {

  val readDb = pgPool(vertx, "READ", config)
  val writeDb = pgPool(vertx, "WRITE", config)
  private val entities = mutableMapOf<String, EntityComponent<out Entity>>()

  companion object {
    val log: Logger = LoggerFactory.getLogger(CrabzillaApplication::class.java)
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

  fun addProjector(name: String, eventProjector: PgcEventProjector) {
    log.info("adding projector $name")
    val uolProjector = PgcUowProjector(readDb, name)
    vertx.eventBus().consumer<UnitOfWorkEvents>(projectionEndpoint) { message ->
      uolProjector.handle(message.body(), eventProjector, Handler { result ->
        if (result.failed()) {
          log.error("Projection [$name] failed: " + result.cause().message)
        }
      })
    }
  }

  fun closeDatabases() {
    writeDb.close()
    readDb.close()
  }

}
