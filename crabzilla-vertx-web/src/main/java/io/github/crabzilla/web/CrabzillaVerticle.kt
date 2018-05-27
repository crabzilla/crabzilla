package io.github.crabzilla.vertx.verticles

import io.github.crabzilla.Command
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.vertx.CommandExecution
import io.github.crabzilla.vertx.CommandExecution.RESULT
import io.github.crabzilla.vertx.CommandHandlerService
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.VerticleRole.REST
import io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory.getLogger
import java.util.*


// TODO add circuit breakers
// TODO add endpoints for list/start/stop crabzilla verticles
class CrabzillaVerticle(override val name: String,
                        private val config: JsonObject,
                        private val healthCheckHandler : HealthCheckHandler,
                        private val uowRepository: UnitOfWorkRepository,
                        private val handlerService: CommandHandlerService)
  : CrabzillaVerticle(name, REST) {


  companion object {
    internal var log = getLogger(CrabzillaVerticle::class.java)
  }

  override fun start() {

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route("/health").handler(healthCheckHandler)

    router.post("/:resource/commands")
            .handler({ this.postCommandHandler(it) })

    router.get("/:resource/commands/:cmdID")
            .handler({ this.getUowByCmdId(it) })

    val server = vertx.createHttpServer()
    val port = config.getInteger("HTTP_PORT")!!

    server.requestHandler({ router.accept(it) })
      .listen(port, { result ->
        if (result.succeeded()) {
          log.info("*** server $name on port $port")
        } else {
          log.error("*** server $name on port $port")
        }
      })

  }

  private fun postCommandHandler(routingContext: RoutingContext) {

    val uowFuture = Future.future<UnitOfWork>()
    val httpResp = routingContext.response()
    val commandStr = routingContext.bodyAsString
    val command = Json.decodeValue(commandStr, Command::class.java)
    val resource = routingContext.request().getParam("resource")

    log.info("command=:\n" + commandStr)

    if (command == null) {
      httpResp.setStatusCode(400).headers().add("Content-Type", "application/json")
      httpResp.end(Json.encode(listOf("invalid command")))
      return
    }

    val cmdID = command.commandId.toString()

    uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

    uowFuture.setHandler { uowResult ->
      if (uowResult.failed()) {
        httpResp.setStatusCode(500).end()
        return@setHandler
      }

      if (uowResult.result() != null) {
        val location = (routingContext.request().absoluteURI() + "/" + uowResult.result().command.commandId.toString())
        httpResp.setStatusCode(201).headers().add("Location", location).add("Content-Type", "application/json")
        httpResp.end(Json.encode(listOf<String>()))
        return@setHandler
      }

      handlerService.postCommand(cmdHandlerEndpoint(resource), command, cmdHandler(routingContext, httpResp))

    }

  }

  private fun cmdHandler(routingContext: RoutingContext, httpResp: HttpServerResponse):
    Handler<AsyncResult<CommandExecution>> {

    return Handler { response ->

      httpResp.headers().add("Content-Type", "application/json")

      if (!response.succeeded()) {
        log.error("eventbus.handleCommand", response.cause())
        httpResp.setStatusCode(500).end(Json.encode(listOf("server error")))
        return@Handler
      }

      val result = response.result() as CommandExecution
      log.info("*** result = {}", result)

      when (result.result) {
        RESULT.SUCCESS -> {
          val location = routingContext.request().absoluteURI() + "/" + result.unitOfWork!!
            .command.commandId.toString()
          httpResp.setStatusCode(201).headers().add("Location", location)
          httpResp.end(Json.encode(listOf<String>()))
        }
        RESULT.VALIDATION_ERROR -> {
          httpResp.statusCode = 400
          httpResp.end(Json.encode(result.constraints))
        }
        RESULT.HANDLING_ERROR -> {
          httpResp.statusCode = 400
          httpResp.end(Json.encode(result.constraints))
        }
        RESULT.UNKNOWN_COMMAND -> {
          httpResp.statusCode = 400
          httpResp.end(Json.encode(listOf("unknown command")))
        }
        else -> {
          httpResp.statusCode = 500
          httpResp.end(Json.encode(listOf("server error")))
        }

      }

    }

  }

  private fun getUowByCmdId(routingContext: RoutingContext) {

    val httpResp = routingContext.response()
    val cmdID = routingContext.request().getParam("cmdID")

    if (cmdID == null) {
      httpResp.setStatusCode(400).end()
      return
    }

    val uowFuture = Future.future<UnitOfWork>()

    uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

    uowFuture.setHandler { uowResult ->
      if (uowResult.failed()) {
        httpResp.statusCode = 500
        httpResp.end()
        return@setHandler
      }

      if (uowResult.result() == null) {
        httpResp.statusCode = 404
        httpResp.end()
        return@setHandler
      } else {
        httpResp.setStatusCode(200).headers().add("Content-Type", "application/json")
        httpResp.end(Json.encode(uowResult.result()))
      }

    }

  }

}



