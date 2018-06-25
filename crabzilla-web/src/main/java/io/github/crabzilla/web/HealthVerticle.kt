package io.github.crabzilla.web

import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.VerticleRole.REST
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory.getLogger

class HealthVerticle(override val name: String,
                     private val port: Int,
                     private val healthCheckHandler : HealthCheckHandler)
  : CrabzillaVerticle(name + "health", REST) {


  companion object {
    internal var log = getLogger(HealthVerticle::class.java)
  }

  override fun start() {

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route("/health").handler(healthCheckHandler)

    val server = vertx.createHttpServer()

    server.requestHandler({ router.accept(it) })
      .listen(port, { result ->
        if (result.succeeded()) {
          log.info("*** server $name on port $port")
        } else {
          log.error("*** server $name on port $port")
        }
      })

  }

}



