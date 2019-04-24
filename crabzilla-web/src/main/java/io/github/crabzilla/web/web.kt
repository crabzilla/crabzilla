package io.github.crabzilla.web

import io.github.crabzilla.*
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.util.*

const val CONTENT_TYPE_UNIT_OF_WORK_ID = "application/vnd.crabzilla.unit_of_work_id+json"
const val CONTENT_TYPE_UNIT_OF_WORK_BODY = "application/vnd.crabzilla.unit_of_work+json"

const val COMMAND_NAME = "commandName"
const val COMMAND_ENTITY_ID = "entityId"
const val COMMAND_ENTITY_RESOURCE = "entityResource"
const val UNIT_OF_WORK_ID = "unitOfWorkId"

private val log = LoggerFactory.getLogger("crabzilla.web")

fun postCommandHandler(routingCtx: RoutingContext, resourceToEntity: (String) -> String, projectionEndpoint: String) {

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

  val postCmd = CommandMetadata(resourceToEntity(routingCtx.pathParam(COMMAND_ENTITY_RESOURCE)),
    routingCtx.pathParam(COMMAND_ENTITY_ID).toInt(), routingCtx.pathParam(COMMAND_NAME))

  httpResp.headers().add("Content-Type", "application/json")

  log.info("posting a command to $postCmd")

  val handlerEndpoint = CommandHandlerEndpoint(postCmd.entityName).endpoint()

  routingCtx.vertx().eventBus()
    .send<Pair<UnitOfWork, Int>>(handlerEndpoint, Pair(postCmd, commandJson)) { response ->

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

    val location = routingCtx.request().absoluteURI().split('/').subList(0, 3)
      .reduce { acc, s ->  acc.plus("/$s")} + "/units-of-work/${result.first.unitOfWorkId}"

    httpResp
      .putHeader("accept", routingCtx.request().getHeader("accept"))
      .putHeader("Location", location)
      .setStatusCode(303)
      .end()

  }

}

fun getUowHandler(rc: RoutingContext, uowRepo: UnitOfWorkRepository) {

  val httpResp = rc.response()
  val unitOfWorkId = rc.request().getParam(UNIT_OF_WORK_ID)

  if (unitOfWorkId == null) {
    httpResp.setStatusCode(400).end()
    return
  }

  val uowFuture = Future.future<UnitOfWork>()

  uowRepo.getUowByUowId(UUID.fromString(unitOfWorkId), uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed() || uowResult.result() == null) {
      httpResp.statusCode = if (uowResult.result() == null) 404 else 500; httpResp.end()
    } else {
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

}
