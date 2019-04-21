package io.github.crabzilla.web

import io.github.crabzilla.JsonMetadata.COMMAND_ID
import io.github.crabzilla.JsonMetadata.COMMAND_JSON_CONTENT
import io.github.crabzilla.JsonMetadata.COMMAND_NAME
import io.github.crabzilla.JsonMetadata.COMMAND_TARGET_ID
import io.github.crabzilla.ProjectionData
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.UnitOfWorkRepository
import io.github.crabzilla.cmdHandlerEndpoint
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.util.*
import io.github.crabzilla.JsonMetadata as JsonMetadata1

val CONTENT_TYPE_UNIT_OF_WORK_ID = "application/vnd.crabzilla.unit_of_work_id+json"
val CONTENT_TYPE_UNIT_OF_WORK_BODY = "application/vnd.crabzilla.unit_of_work+json"

private val log = LoggerFactory.getLogger("crabzilla.web")

fun postCommandHandler(routingCtx: RoutingContext, uowRepository: UnitOfWorkRepository, projectionEndpoint: String) {

  val uowFuture = Future.future<UnitOfWork>()
  val httpResp = routingCtx.response()

  val commandJson = routingCtx.bodyAsJson

  if (commandJson == null) {
    httpResp
      .setStatusCode(400)
      .setStatusMessage("invalid command")
      .end()
    return
  }

  log.info("command=:\n${commandJson.encode()}")

  val cmdID = UUID.randomUUID().toString()

  val jo = JsonObject()
  jo.put(COMMAND_TARGET_ID, Integer(routingCtx.pathParam(COMMAND_TARGET_ID)))
  jo.put(COMMAND_ID, cmdID)
  jo.put(COMMAND_NAME, routingCtx.pathParam(COMMAND_NAME))
  jo.put(COMMAND_JSON_CONTENT, commandJson)

  val resource = routingCtx.request().getParam("resource")

  httpResp.headers().add("Content-Type", "application/json")

  uowRepository.getUowByCmdId(UUID.fromString(cmdID), uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed()) {
      httpResp.setStatusCode(500).setStatusMessage("server error").end()
      return@setHandler
    }

    val location = routingCtx.request().absoluteURI().split('/').subList(0, 3)
      .reduce { acc, s ->  acc.plus("/$s")} + "/commands/$cmdID"

    if (uowResult.result() != null) {
      httpResp.putHeader("accept", routingCtx.request().getHeader("accept"))
              .putHeader("Location", location)
              .setStatusCode(303)
              .end()
      return@setHandler
    }

    val handlerEndpoint = cmdHandlerEndpoint(resource)

    log.info("posting a command to $handlerEndpoint")

    routingCtx.vertx().eventBus()
      .send<Pair<UnitOfWork, Int>>(handlerEndpoint, jo) { response ->

      if (!response.succeeded()) {
        val cause = response.cause() as ReplyException
        httpResp.setStatusCode(cause.failureCode()).setStatusMessage(cause.message).end()
        return@send
      }

      val result = response.result().body() as Pair<UnitOfWork, Int>

      log.info("result = {}", result)

      val headers = CaseInsensitiveHeaders().add("uowSequence", result.second.toString())
      val eventsDeliveryOptions = DeliveryOptions().setCodecName("UnitOfWork").setHeaders(headers)

      routingCtx.vertx().eventBus()
      .publish(projectionEndpoint, ProjectionData.fromUnitOfWork(result.second, result.first), eventsDeliveryOptions)

      httpResp
        .putHeader("accept", routingCtx.request().getHeader("accept"))
        .putHeader("Location", location)
        .setStatusCode(303)
        .end()

    }

  }

}

fun getUowByCmdIdHandler(rc: RoutingContext, uowRepo: UnitOfWorkRepository) {

  val httpResp = rc.response()
  val cmdID = rc.request().getParam(COMMAND_ID)

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
    httpResp.setStatusCode(200).setChunked(true).
      headers().add("Content-Type", "application/json")
    val defaultResult = JsonObject().put("unitOfWorkId", uowResult.result().unitOfWorkId.toString())
    val effectiveResult: JsonObject = when (contentType) {
      CONTENT_TYPE_UNIT_OF_WORK_BODY -> JsonObject.mapFrom(uowResult.result())
      else -> defaultResult
    }
    httpResp.end(effectiveResult.encode())
  }

}


