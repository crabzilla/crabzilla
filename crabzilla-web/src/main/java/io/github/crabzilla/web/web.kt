package io.github.crabzilla.web

import io.github.crabzilla.*
import io.github.crabzilla.web.ContentTypes.UNIT_OF_WORK_BODY
import io.github.crabzilla.web.ContentTypes.UNIT_OF_WORK_ID
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.http.CaseInsensitiveHeaders
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.util.*

object ContentTypes {
  const val UNIT_OF_WORK_ID = "application/vnd.crabzilla.unit-of-work-id+json"
  const val UNIT_OF_WORK_BODY = "application/vnd.crabzilla.unit-of-work+json"
  const val ENTITY_TRACKING = "application/vnd.crabzilla.entity-tracking+json"
}

private const val UNIT_OF_WORK_ID_PATH_PARAMETER = "unitOfWorkId"

private val log = LoggerFactory.getLogger("crabzilla.web")

fun postCommandHandler(rc: RoutingContext, commandMetadata: CommandMetadata,
                       projectionEndpoint: String) {

  val httpResp = rc.response()

  val commandJson = rc.bodyAsJson

  if (commandJson == null) {
    httpResp.setStatusCode(400).setStatusMessage("invalid command").end(); return
  }

  log.trace("command/metadata=:\n${commandJson.encode()}\n$commandMetadata")

  httpResp.headers().add("Content-Type", "application/json")

  val handlerEndpoint = CommandHandlerEndpoint(commandMetadata.entityName).endpoint()

  rc.vertx().eventBus()
    .send<Pair<UnitOfWork, Int>>(handlerEndpoint, Pair(commandMetadata, commandJson)) { response ->

    if (response.failed() || response.result().body() == null) {
      val cause = response.cause() as ReplyException
      log.error("when sending command to handler via event bus", cause)
      httpResp.setStatusCode(cause.failureCode()).setStatusMessage(cause.message).end()
      return@send
    }

    val result = response.result().body() as Pair<UnitOfWork, Int>

    with(result) {

      val headers = CaseInsensitiveHeaders().add("uowSequence", second.toString())
      val eventsDeliveryOptions = DeliveryOptions().setHeaders(headers)

      rc.vertx().eventBus()
        .publish(projectionEndpoint, ProjectionData.fromUnitOfWork(second, first), eventsDeliveryOptions)

      val contentType = rc.request().getHeader("accept")
      val resultJson = when (contentType) {
        UNIT_OF_WORK_BODY -> JsonObject.mapFrom(result.first)
        UNIT_OF_WORK_ID -> JsonObject().put(UNIT_OF_WORK_ID_PATH_PARAMETER, result.first.unitOfWorkId.toString())
        else -> JsonObject()
      }

      log.info("content-type: ${rc.request().getHeader("accept")}")
      log.info("result: ${resultJson}")

      httpResp
        .putHeader("accept", rc.request().getHeader("accept"))
        .putHeader("Content-Type", "application/json")
        .setStatusCode(201)
        .end(resultJson.encode())

    }

  }

}

fun getUowHandler(rc: RoutingContext, uowRepo: UnitOfWorkRepository, unitOfWorkId: UUID) {

  val httpResp = rc.response()

  val uowFuture = Future.future<UnitOfWork>()
  uowRepo.getUowByUowId(unitOfWorkId, uowFuture)

  uowFuture.setHandler { uowResult ->
    if (uowResult.failed() || uowResult.result() == null) {
      httpResp.statusCode = if (uowResult.result() == null) 404 else 500; httpResp.end()
    } else {
      val contentType = rc.request().getHeader("accept")
      httpResp.setStatusCode(200).setChunked(true).
        headers().add("Content-Type", "application/json")
      val effectiveResult: JsonObject = when (contentType) {
        UNIT_OF_WORK_BODY -> JsonObject.mapFrom(uowResult.result())
        UNIT_OF_WORK_ID -> JsonObject().put(UNIT_OF_WORK_ID_PATH_PARAMETER, uowResult.result().unitOfWorkId.toString())
        else -> JsonObject()
      }
      httpResp.end(effectiveResult.encode())
    }
  }

}

fun <E : Entity> entityTrackingHandler(rc: RoutingContext,
                                       entityId: Int,
                                       uowRepo: UnitOfWorkRepository,
                                       snapshotRepo: SnapshotRepository<E>,
                                       entityToJson: (E) -> JsonObject) {

  val httpResp = rc.response()

  val snapshotFuture = Future.future<Snapshot<E>>()
  snapshotRepo.retrieve(entityId, snapshotFuture)

  val uowListFuture = Future.future<List<UnitOfWork>>()
  uowRepo.getAllUowByEntityId(entityId, uowListFuture)

  CompositeFuture.all(snapshotFuture, uowListFuture).setHandler { event ->
    if (event.failed()) {
      httpResp.setStatusCode(500).end("Server error")
      return@setHandler
    }

    val result = JsonObject()
    val snapshot = snapshotFuture.result()
    val snapshotJson = JsonObject().put("version", snapshot.version).put("state", entityToJson.invoke(snapshot.state))
    if (snapshot.version > 0) result.put("snapshot", snapshotJson)
    val uowList = uowListFuture.result()
    if (uowList.isNotEmpty()) result.put("units_of_work", JsonArray(uowListFuture.result()))
    if (result.isEmpty) {
      httpResp.setStatusCode(404).end("Entity not found")
    }
    val response = httpResp.setStatusCode(200).setChunked(true)
    response.headers().add("Content-Type", "application/json")
    response.end(result.encode())
  }

}
