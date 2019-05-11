package io.github.crabzilla.web

import io.github.crabzilla.*
import io.github.crabzilla.web.ContentTypes.UNIT_OF_WORK_BODY
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

private const val UNIT_OF_WORK_ID = "unitOfWorkId"

private val log = LoggerFactory.getLogger("crabzilla.web")

fun postCommandHandler(routingCtx: RoutingContext, commandMetadata: CommandMetadata,
                       projectionEndpoint: String) {

  val httpResp = routingCtx.response()

  val commandJson = routingCtx.bodyAsJson

  if (commandJson == null) {
    httpResp.setStatusCode(400).setStatusMessage("invalid command").end(); return
  }

  log.info("command=:\n${commandJson.encode()}")

  httpResp.headers().add("Content-Type", "application/json")

  log.info("posting a command to $commandMetadata")

  val handlerEndpoint = CommandHandlerEndpoint(commandMetadata.entityName).endpoint()

  routingCtx.vertx().eventBus()
    .send<Pair<UnitOfWork, Int>>(handlerEndpoint, Pair(commandMetadata, commandJson)) { response ->

    if (!response.succeeded()) {
      val cause = response.cause() as ReplyException
      httpResp.setStatusCode(cause.failureCode()).setStatusMessage(cause.message).end() ;
      return@send
    }

    val result = response.result().body() as Pair<UnitOfWork, Int>

    log.info("result = {}", result)

    with(result) {

      val headers = CaseInsensitiveHeaders().add("uowSequence", second.toString())
      val eventsDeliveryOptions = DeliveryOptions().setHeaders(headers)

      routingCtx.vertx().eventBus()
        .publish(projectionEndpoint, ProjectionData.fromUnitOfWork(second, first), eventsDeliveryOptions)

      val location = routingCtx.request().absoluteURI().split('/').subList(0, 3)
        .reduce { acc, s ->  acc.plus("/$s")} + "/units-of-work/${first.unitOfWorkId}"

      httpResp
        .putHeader("accept", routingCtx.request().getHeader("accept"))
        .putHeader("Content-Type", "application/json")
        .putHeader("Location", location)
        .setStatusCode(303)
        .end()

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
      val defaultResult = JsonObject().put(UNIT_OF_WORK_ID, uowResult.result().unitOfWorkId.toString())
      val effectiveResult: JsonObject = when (contentType) {
        UNIT_OF_WORK_BODY -> JsonObject.mapFrom(uowResult.result())
        else -> defaultResult
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
