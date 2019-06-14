package io.github.crabzilla.stack1.example1

import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.stack1.CrabzillaWebVerticle
import io.github.crabzilla.stack1.CrabzillaApplication
import io.vertx.core.Launcher
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1Verticle::class.java.name)
}


class Example1Verticle(override val httpPort: Int, override val configFile: String = "./example1.env")
  : CrabzillaWebVerticle() {

  lateinit var crablet: CrabzillaApplication

  override fun startCrabzilla(config: JsonObject, router: Router) {

    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // example1
    crablet = CrabzillaApplication(vertx, router, config, "example1")
    crablet.addEntity("customer", CustomerJsonAware(), CustomerCommandAware())
    crablet.addWebResource("customers", "customer")
    crablet.addProjector("customer-summary", CustomerSummaryProjector())

    // read model routes
    router.get("/customers/:id").handler {
      it.response()
        .putHeader("Content-type", "application/json")
        .end(JsonObject().put("message", "TODO query read model").encode()) // TODO
    }

  }

  override fun stop() {
    log.info("*** closing resources")
    crablet.closeDatabases()
    server.close()
  }

}

