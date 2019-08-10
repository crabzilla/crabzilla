package io.github.crabzilla.stack1.example1

import io.github.crabzilla.UnitOfWorkEvents
import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.stack1.Stack1SimpleVerticle
import io.github.crabzilla.stack1.Stack1WebApp
import io.vertx.core.Handler
import io.vertx.core.Launcher
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1Verticle::class.java.name)
}

// override val httpPort: Int, override val configFile: String = "./example1.env"
class Example1Verticle : Stack1SimpleVerticle() {

  lateinit var crablet: Stack1WebApp

  override fun startCrabzilla(config: JsonObject, router: Router): Stack1WebApp {

    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // example1
    crablet = Stack1WebApp(vertx, router, config, projectionEndpoint = "example1")
    crablet.addEntity("customer", CustomerJsonAware(), CustomerCommandAware())
    crablet.addWebResource(resourceName = "customers", entityName = "customer")

    val projectionName = "customer-summary"
    log.info("adding projector for $projectionName")
    val uolProjector = PgcUowProjector(crablet.readDb, projectionName)
    val summaryProjector = CustomerSummaryProjector()
    vertx.eventBus().consumer<UnitOfWorkEvents>(crablet.projectionEndpoint) { message ->
      uolProjector.handle(message.body(), summaryProjector, Handler { result ->
        if (result.failed()) { // TODO circuit breaker
          log.error("Projection [$projectionName] failed: " + result.cause().message)
        }
      })
    }

    // read model routes
    router.get("/customers/:id").handler {
      it.response()
        .putHeader("Content-type", "application/json")
        .end(JsonObject().put("message", "TODO query read model").encode()) // TODO
    }

    return crablet

  }

}

