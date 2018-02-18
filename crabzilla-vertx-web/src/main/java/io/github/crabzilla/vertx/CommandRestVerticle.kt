package io.github.crabzilla.vertx

import io.github.crabzilla.core.EntityCommand
import io.github.crabzilla.core.UnitOfWork
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
class CommandRestVerticle(override val name: String,
                          private val config: JsonObject,
                          private val healthCheckHandler : HealthCheckHandler,
                          private val uowRepository: UnitOfWorkRepository,
                          private val handlerService: CommandHandlerService)
  : CrabzillaVerticle(name, REST) {


  companion object {
    internal var log = getLogger(CommandHandlerService::class.java)
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

    log.info("*** server on port ${port}")

    server.requestHandler({ router.accept(it) })
            .listen(port)

  }

  private fun postCommandHandler(routingContext: RoutingContext) {

    val uowFuture = Future.future<UnitOfWork>()
    val httpResp = routingContext.response()
    val commandStr = routingContext.bodyAsString
    val command = Json.decodeValue(commandStr, EntityCommand::class.java)
    val resource = routingContext.request().getParam("resource")

    log.info("command=:\n" + commandStr)

    if (command == null) {
      httpResp.setStatusCode(400).end()
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
        val location = (routingContext.request().absoluteURI() + "/"
          + uowResult.result().command.commandId.toString())
        val resultAsJson = Json.encode(uowResult.result())
        httpResp.setStatusCode(201).headers().add("Location", location).add("Content-Type", "application/json")
        httpResp.end(resultAsJson)
        return@setHandler
      }

      handlerService.postCommand(cmdHandlerEndpoint(resource), command, cmdHandler(routingContext, httpResp))

    }

  }

  private fun cmdHandler(routingContext: RoutingContext, httpResp: HttpServerResponse):
    Handler<AsyncResult<CommandExecution>> {

    return Handler { response ->

        if (!response.succeeded()) {
          log.error("eventbus.handleCommand", response.cause())
          httpResp.setStatusCode(500).end(response.cause().message)
          return@Handler
        }

        val result = response.result() as CommandExecution
        log.info("result = {}", result)

        when (result.result) {
          CommandExecution.RESULT.SUCCESS -> {
            val location = routingContext.request().absoluteURI() + "/" + result.unitOfWork!!
              .command.commandId.toString()
            httpResp.setStatusCode(201).headers().add("Content-Type", "application/json")
              .add("Location", location)
          }
          CommandExecution.RESULT.VALIDATION_ERROR -> {
            httpResp.setStatusCode(400).headers().add("Content-Type", "application/json")
            httpResp.write(Json.encode(result.constraints))
          }
          CommandExecution.RESULT.UNKNOWN_COMMAND -> {
            httpResp.statusCode = 400
          }
          else -> httpResp.statusCode = 500
        }

        httpResp.end()
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
        httpResp.setStatusCode(500).end()
      }

      if (uowResult.result() == null) {
        httpResp.setStatusCode(400).end()
      } else {
        val resultAsJson = Json.encode(uowResult.result())
        httpResp.setStatusCode(200).headers().add("Content-Type", "application/json")
        httpResp.end(resultAsJson)
      }

    }

  }

}



