package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.VerticleRole.REST
import io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT
import io.github.crabzilla.vertx.entity.impl.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint
import io.github.crabzilla.vertx.helpers.EndpointsHelper.restEndpoint
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory.getLogger
import java.util.*


// TODO add circuit breakers and healthchecks
// TODO add endpoints for list/start/stop crabzilla verticles
class EntityCommandRestVerticle(private val entityName: String,
                                private val config: JsonObject,
                                private val uowRepository: EntityUnitOfWorkRepositoryImpl,
                                private val handlerService: EntityCommandHandlerService) : CrabzillaVerticle(entityName, REST) {

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
    val port = config.getInteger("HTTP_PORT")!!

    log.info("*** server on port ${port}")

    server.requestHandler({ router.accept(it) })
            .listen(port)

  }

  private fun postCommandHandler(routingContext: RoutingContext) {

    val uowFuture = Future.future<EntityUnitOfWork>()
    val httpResp = routingContext.response()
    val commandStr = routingContext.bodyAsString
    val command = Json.decodeValue(commandStr, EntityCommand::class.java)

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

      handlerService.postCommand(cmdHandlerEndpoint(entityName), command, { response ->

        if (!response.succeeded()) {
          log.error("eventbus.handleCommand", response.cause())
          httpResp.setStatusCode(500).end(response.cause().message)
          return@postCommand
        }

        val result = response.result() as EntityCommandExecution
        log.info("result = {}", result)

        if (result.result == RESULT.SUCCESS) {
          val location = routingContext.request().absoluteURI() + "/" + result.unitOfWork!!
            .command.commandId.toString()
          httpResp.setStatusCode(201).headers().add("Location", location).add("Content-Type", "application/json")
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

  private fun getUowByCmdId(routingContext: RoutingContext) {

    val httpResp = routingContext.response()
    val cmdID = routingContext.request().getParam("cmdID")

    if (cmdID == null) {
      httpResp.setStatusCode(400).end()
      return
    }

    val uowFuture = Future.future<EntityUnitOfWork>()

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

  companion object {
    internal var log = getLogger(EntityCommandHandlerService::class.java)
  }

}
