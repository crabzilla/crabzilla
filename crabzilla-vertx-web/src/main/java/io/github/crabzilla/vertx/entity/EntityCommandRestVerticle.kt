package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.vertx.EntityCommandExecution
import io.github.crabzilla.vertx.EntityCommandExecution.RESULT
import io.github.crabzilla.vertx.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.entity.service.EntityCommandHandlerService
import io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint
import io.github.crabzilla.vertx.helpers.EndpointsHelper.restEndpoint
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory.getLogger
import java.util.*

// TODO circuit breakers and healthchecks
class EntityCommandRestVerticle(private val entityName: String,
                                private val config: JsonObject,
                                private val uowRepository: EntityUnitOfWorkRepositoryImpl,
                                private val handlerService: EntityCommandHandlerService) : AbstractVerticle() {

  override fun start() {

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route("/ping").handler {
      routingContext ->
      run {
        routingContext.response().putHeader("content-type", "text/plain").end("pong")
        log.info("*** pong")
      }
    }

    router.post("/" + restEndpoint(entityName) + "/commands")
            .handler({ this.postCommandHandler(it) })

    router.get("/" + restEndpoint(entityName) + "/commands/:cmdID")
            .handler({ this.getUowByCmdId(it) })

    val server = vertx.createHttpServer()

    val port = config.getInteger("http.port")!!

    log.info("*** server on port ${port}")

    server.requestHandler({ router.accept(it) })
            .listen(port)

  }

  internal fun postCommandHandler(routingContext: RoutingContext) {

    val httpResp = routingContext.response()
    val commandStr = routingContext.bodyAsString

    log.info("command=:\n" + commandStr)

    val command = Json.decodeValue(commandStr, EntityCommand::class.java)

    if (command == null) {
      sendError(400, httpResp)
      return
    }

    val cmdID = command.commandId.toString()

    val uowFuture = Future.future<EntityUnitOfWork>()

    uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

    uowFuture.setHandler { uowResult ->
      if (uowResult.failed()) {
        sendError(500, httpResp)
        return@setHandler
      }

      if (uowResult.result() != null) {
        httpResp.statusCode = 201
        val location = (routingContext.request().absoluteURI() + "/"
                + uowResult.result().command.commandId.toString())
        httpResp.headers().add("Location", location)
        val resultAsJson = Json.encode(uowResult.result())
        httpResp.headers().add("Content-Type", "application/json")
        httpResp.end(resultAsJson)
        return@setHandler
      }

      handlerService.postCommand(cmdHandlerEndpoint(entityName), command, { response ->

        if (!response.succeeded()) {
          log.error("eventbus.handleCommand", response.cause())
          httpResp.statusCode = 500
          httpResp.end(response.cause().message)
          return@postCommand
        }

        val result = response.result() as EntityCommandExecution
        log.info("result = {}", result)

        if (result.result == RESULT.SUCCESS) {
          httpResp.statusCode = 201
          val location = routingContext.request().absoluteURI() + "/" + result.unitOfWork!!
            .command.commandId.toString()
          httpResp.headers().add("Location", location)
        } else {
          if (result.result == RESULT.VALIDATION_ERROR || result.result == RESULT.UNKNOWN_COMMAND) {
            httpResp.statusCode = 400
          } else {
            httpResp.statusCode = 500
          }
        }
        httpResp.end()

      })

    }

  }

  internal fun getUowByCmdId(routingContext: RoutingContext) {

    val httpResp = routingContext.response()
    val cmdID = routingContext.request().getParam("cmdID")

    if (cmdID == null) {
      sendError(400, httpResp)
      return
    }

    val uowFuture = Future.future<EntityUnitOfWork>()

    uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

    uowFuture.setHandler { uowResult ->
      if (uowResult.failed()) {
        sendError(500, httpResp)
      }

      if (uowResult.result() == null) {
        sendError(404, httpResp)
      } else {
        val resultAsJson = Json.encode(uowResult.result())
        httpResp.headers().add("Content-Type", "application/json")
        httpResp.end(resultAsJson)
      }

    }

  }

  private fun sendError(statusCode: Int, response: HttpServerResponse) {
    response.setStatusCode(statusCode).end()
  }

  companion object {

    internal var log = getLogger(EntityCommandHandlerService::class.java)
  }


}
