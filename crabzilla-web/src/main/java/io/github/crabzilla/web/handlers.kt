package io.github.crabzilla.web

import io.github.crabzilla.Command
import io.github.crabzilla.CommandExecution
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.commandFromJson
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.cmdHandlerEndpoint
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import java.util.*


private val log = LoggerFactory.getLogger("WebExtensions")

private val commandDeliveryOptions = DeliveryOptions().setCodecName(Command::class.java.simpleName)

fun postCommandHandler(routingContext: RoutingContext, uowRepository: UnitOfWorkRepository,
                       projectionEndpoint: String) {

  val uowFuture = Future.future<UnitOfWork>()
  val httpResp = routingContext.response()
  val commandStr = routingContext.bodyAsString
  val command = commandFromJson(Json.mapper, commandStr)
  val resource = routingContext.request().getParam("resource")

  log.info("command=:\n$commandStr")

  httpResp.headers().add("Content-Type", "application/json")

  if (command == null) {
    httpResp.statusCode = 400
    httpResp.end(Json.encode(listOf("invalid command")))
    return
  }

  val cmdID = command.commandId.toString()

  uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed()) {
      httpResp.setStatusCode(500).end(Json.encode(listOf("server error")))
      return@setHandler
    }

    if (uowResult.result() != null) {
      val location = (routingContext.request().absoluteURI() + "/" + uowResult.result().command.commandId.toString())
      httpResp.setStatusCode(201).headers().add("Location", location)
      httpResp.end(Json.encode(listOf<String>()))
      return@setHandler
    }

    val handlerEndpoint = cmdHandlerEndpoint(resource)

    log.info("posting a command to {}", handlerEndpoint)

    routingContext.vertx().eventBus().send<Command>(handlerEndpoint, command, commandDeliveryOptions) { response ->

      if (!response.succeeded()) {
        log.error("postCommand", response.cause())
        httpResp.setStatusCode(500).end(Json.encode(listOf("server error")))
        return@send
      }

      val result = response.result().body() as CommandExecution
      val uow = result.unitOfWork
      val uowSequence = result.uowSequence ?: 0

      log.info("result = {}", result)

      if (uow != null && uowSequence != 0) {
        val headers = CaseInsensitiveHeaders().add("uowSequence", uowSequence.toString())
        val eventsDeliveryOptions = DeliveryOptions().setCodecName(UnitOfWork::class.simpleName).setHeaders(headers)
        routingContext.vertx().eventBus()
          .publish(projectionEndpoint, ProjectionData.fromUnitOfWork(uowSequence, uow), eventsDeliveryOptions)

      }

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

}

fun getUowByCmdIdHandler(routingContext: RoutingContext, uowRepository: UnitOfWorkRepository) {

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

