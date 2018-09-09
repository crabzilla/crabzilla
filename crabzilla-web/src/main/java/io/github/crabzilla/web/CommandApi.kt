package io.github.crabzilla.web

import io.github.crabzilla.Command
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.vertx.CommandExecution
import io.github.crabzilla.vertx.CommandHandlerService
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.helpers.EndpointsHelper
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import java.util.*


internal var log = LoggerFactory.getLogger("CommandApi")

fun postCommandHandler(routingContext: RoutingContext, uowRepository: UnitOfWorkRepository,
                       handlerService: CommandHandlerService) {

  val uowFuture = Future.future<UnitOfWork>()
  val httpResp = routingContext.response()
  val commandStr = routingContext.bodyAsString
  val command = Json.decodeValue(commandStr, Command::class.java)
  val resource = routingContext.request().getParam("resource")

  log.info("command=:\n$commandStr")

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

    handlerService.postCommand(EndpointsHelper.cmdHandlerEndpoint(resource), command,
                               resultHandler(routingContext, httpResp))

  }

}

fun resultHandler(routingContext: RoutingContext, httpResp: HttpServerResponse):
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
      CommandExecution.RESULT.SUCCESS -> {
        val location = routingContext.request().absoluteURI() + "/" + result.unitOfWork!!
          .command.commandId.toString()
        httpResp.setStatusCode(201).headers().add("Location", location)
        httpResp.end(Json.encode(listOf<String>()))
      }
      CommandExecution.RESULT.VALIDATION_ERROR -> {
        httpResp.statusCode = 400
        httpResp.end(Json.encode(result.constraints))
      }
      CommandExecution.RESULT.HANDLING_ERROR -> {
        httpResp.statusCode = 400
        httpResp.end(Json.encode(result.constraints))
      }
      CommandExecution.RESULT.UNKNOWN_COMMAND -> {
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

fun getUowByCmdId(routingContext: RoutingContext, uowRepository: UnitOfWorkRepository) {

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
