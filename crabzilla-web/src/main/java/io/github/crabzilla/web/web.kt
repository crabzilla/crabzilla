package io.github.crabzilla.web

import io.github.crabzilla.*
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.util.*

val CONTENT_TYPE_UNIT_OF_WORK_ID = "application/vnd.crabzilla.unit_of_work_id+json"
val CONTENT_TYPE_UNIT_OF_WORK_BODY = "application/vnd.crabzilla.unit_of_work+json"

private val log = LoggerFactory.getLogger("crabzilla.web")

private val commandDeliveryOptions = DeliveryOptions().setCodecName(Command::class.java.simpleName)

fun postCommandHandler(routingContext: RoutingContext, uowRepository: UnitOfWorkRepository,
                       projectionEndpoint: String) {

  val uowFuture = Future.future<UnitOfWork>()
  val httpResp = routingContext.response()
  val commandStr = routingContext.bodyAsString

  val command = try { Json.decodeValue(commandStr, Command::class.java) } catch (e:  DecodeException) {null}

  if (command == null) {
    httpResp
      .setStatusCode(400)
      .setStatusMessage("invalid command")
      .end()
    return
  }

  log.info("command=:\n$commandStr")

  val resource = routingContext.request().getParam("resource")

  httpResp.headers().add("Content-Type", "application/json")

  val cmdID = command.commandId.toString()

  uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed()) {
      httpResp.setStatusCode(500).setStatusMessage("server error").end()
      return@setHandler
    }

    val location = (routingContext.request().absoluteURI() + "/" + command.commandId.toString())

    if (uowResult.result() != null) {
      httpResp.putHeader("accept", routingContext.request().getHeader("accept"))
              .putHeader("Location", location)
              .setStatusCode(303)
              .end()
      return@setHandler
    }

    val handlerEndpoint = cmdHandlerEndpoint(resource)

    log.info("posting a command to $handlerEndpoint")

    routingContext.vertx().eventBus().send<Command>(handlerEndpoint, command, commandDeliveryOptions) { response ->

      if (!response.succeeded()) {
        httpResp.setStatusCode(500).setStatusMessage("server error").end()
        return@send
      }

      val result = response.result().body() as CommandExecution

      log.info("result = {}", result)

      val errorResult = if (result.constraints.isEmpty()) JsonObject().encode() else JsonArray(result.constraints).encode()

      when (result.result) {
        CommandExecution.RESULT.SUCCESS -> {
          val uow = result.unitOfWork
          val uowSequence = result.uowSequence ?: 0
          if (uow != null && uowSequence != 0) {
            val headers = CaseInsensitiveHeaders().add("uowSequence", uowSequence.toString())
            val eventsDeliveryOptions = DeliveryOptions().setCodecName(UnitOfWork::class.simpleName).setHeaders(headers)
            routingContext.vertx().eventBus()
              .publish(projectionEndpoint, ProjectionData.fromUnitOfWork(uowSequence, uow), eventsDeliveryOptions)
          }
          httpResp
            .putHeader("accept", routingContext.request().getHeader("accept"))
            .putHeader("Location", location)
            .setStatusCode(303)
            .end()
        }
        CommandExecution.RESULT.VALIDATION_ERROR -> {
          httpResp.setStatusCode(400).setStatusMessage(errorResult).end()
        }
        CommandExecution.RESULT.UNKNOWN_COMMAND -> {
          httpResp.setStatusCode(400).setStatusMessage("unknown command").end()
        }
        else -> {
          httpResp.setStatusCode(500).setStatusMessage(errorResult).end()
        }

      }

    }

  }

}

fun getUowByCmdIdHandler(rc: RoutingContext, uowRepo: UnitOfWorkRepository) {

  val httpResp = rc.response()
  val cmdID = rc.request().getParam("cmdID")

  if (cmdID == null) {
    httpResp.setStatusCode(400).end()
    return
  }

  val uowFuture = Future.future<UnitOfWork>()

  uowRepo.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed() || uowResult.result() == null) {
      httpResp.statusCode = if (uowResult.result() == null) 404 else 500
      httpResp.end()
      return@setHandler
    }
    val contentType = rc.request().getHeader("accept")
    httpResp.setStatusCode(200).headers().add("Content-Type", contentType)
    val result: JsonObject = when (contentType) {
      CONTENT_TYPE_UNIT_OF_WORK_ID -> JsonObject().put("unitOfWorkId", uowResult.result().unitOfWorkId.toString())
      CONTENT_TYPE_UNIT_OF_WORK_BODY -> JsonObject.mapFrom(uowResult.result())
      else -> JsonObject().put("unitOfWorkId", uowResult.result().unitOfWorkId.toString())
    }
    httpResp.end(result.encode())
  }

}


