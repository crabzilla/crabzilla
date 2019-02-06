package io.github.crabzilla.web

import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.VerticleRole.REST
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory.getLogger
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

// TODO add circuit breakers
class WebVerticle(override val name: String,
                  private val config: JsonObject,
                  private val healthCheckHandler : HealthCheckHandler,
                  private val uowRepository: UnitOfWorkRepository,
                  private val projectionEndpoint: String)
  : CrabzillaVerticle(name, REST) {


  companion object {
    internal var log = getLogger(WebVerticle::class.java)
  }

  override fun start() {

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route("/health").handler(healthCheckHandler)

    router.post("/:resource/commands")
            .handler { postCommandHandler(it, uowRepository, projectionEndpoint) }

    router.get("/:resource/commands/:cmdID")
            .handler { getUowByCmdId(it, uowRepository) }

    val port = config.getInteger("HTTP_PORT")!!
    val server = vertx.createHttpServer(HttpServerOptions().setPort(port).setHost("0.0.0.0"))

    server.requestHandler(router).listen { ar ->
      if (ar.succeeded()) {
        log.info("Server started on port " + ar.result().actualPort())
      } else {
        log.error("oops, something went wrong during server initialization", ar.cause())
      }
    }

  }

}



