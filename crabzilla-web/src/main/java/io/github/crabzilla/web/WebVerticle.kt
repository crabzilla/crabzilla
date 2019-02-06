package io.github.crabzilla.web

import io.github.crabzilla.vertx.CommandHandlerService
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.VerticleRole.REST
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
                  private val handlerService: CommandHandlerService)
  : CrabzillaVerticle(name, REST) {


  companion object {
    internal var log = getLogger(WebVerticle::class.java)
  }

  override fun start() {

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route("/health").handler(healthCheckHandler)

    router.post("/:resource/commands")
            .handler { postCommandHandler(it, uowRepository, handlerService) }

    router.get("/:resource/commands/:cmdID")
            .handler {getUowByCmdId(it, uowRepository)}

    val server = vertx.createHttpServer()
    val port = config.getInteger("HTTP_PORT")!!

    server.requestHandler { router.accept(it) }
      .listen(port) { result ->
        if (result.succeeded()) {
          log.info("*** server $name on port $port")
        } else {
          log.error("*** server $name on port $port")
        }
      }

  }

}



