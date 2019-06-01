//package io.github.crabzilla.web
//
//import io.github.crabzilla.*
//import io.github.crabzilla.web.ContentTypes.UNIT_OF_WORK_BODY
//import io.github.crabzilla.web.ContentTypes.UNIT_OF_WORK_ID
//import io.vertx.core.CompositeFuture
//import io.vertx.core.Future
//import io.vertx.core.Handler
//import io.vertx.core.eventbus.DeliveryOptions
//import io.vertx.core.eventbus.ReplyException
//import io.vertx.core.http.CaseInsensitiveHeaders
//import io.vertx.core.json.JsonArray
//import io.vertx.core.json.JsonObject
//import io.vertx.ext.web.RoutingContext
//import org.slf4j.LoggerFactory
//
//private const val UNIT_OF_WORK_ID_PATH_PARAMETER = "unitOfWorkId"
//
//private val log = LoggerFactory.getLogger("CrabzillaWebHandlers")
//
//fun postCommandHandler(rc: RoutingContext, cmdMetadata: CommandMetadata, projectionEndpoint: String) {
////
////  val httpResp = rc.response()
////
////  val commandJson = rc.bodyAsJson
////
////  if (commandJson == null) {
////    httpResp.setStatusCode(400).setStatusMessage("invalid command").end(); return
////  }
////
////  log.trace("command/metadata=:\n${commandJson.encode()}\n$cmdMetadata")
////
////  httpResp.headers().add("Content-Type", "application/json")
////
////  val resultJson = when (rc.request().getHeader("accept")) {
////    UNIT_OF_WORK_BODY -> JsonObject.mapFrom(result.first)
////    UNIT_OF_WORK_ID -> JsonObject().put(UNIT_OF_WORK_ID_PATH_PARAMETER, result.second)
////    else -> JsonObject()
////  }
////
////  httpResp
////    .putHeader("uowId", second.toString())
////    .putHeader("accept", rc.request().getHeader("accept"))
////    .putHeader("Content-Type", "application/json")
////    .setStatusCode(201)
////    .end(resultJson.encode())
//
//}
//
//fun <E : Entity> getUowHandler(rc: RoutingContext, component: EntityComponent<E>, unitOfWorkId: Long) {
//
//  val httpResp = rc.response()
//
//  component.getUowByUowId(unitOfWorkId, Handler { uowResult ->
//    if (uowResult.failed() || uowResult.result() == null) {
//      httpResp.statusCode = if (uowResult.result() == null) 404 else 500
//      httpResp.end()
//    } else {
//      val contentType = rc.request().getHeader("accept")
//      httpResp.setStatusCode(200).setChunked(true).
//        headers().add("Content-Type", "application/json")
//      val effectiveResult: JsonObject = when (contentType) {
//        UNIT_OF_WORK_ID -> JsonObject().put(UNIT_OF_WORK_ID_PATH_PARAMETER, uowResult.result())
//        else -> JsonObject.mapFrom(uowResult.result())
//      }
//      httpResp.end(effectiveResult.encode())
//    }
//  })
//
//}
//
//fun <E : Entity> entityWriteModelHandler(rc: RoutingContext, entityId: Int, component: EntityComponent<E>,
//                                         entityToJson: (E) -> JsonObject) {
//
//  log.info("Retrieving entity write model for $entityId")
//
//  val httpResp = rc.response().setChunked(true)
//
//  component.getSnapshot(entityId, Handler { event ->
//    if (event.failed()) {
//      httpResp.setStatusCode(500).end("Server error")
//      return@Handler
//    }
//
//    val snapshot = event.result()
//    val snapshotJson = JsonObject().put("state", entityToJson.invoke(snapshot.state)).put("version", snapshot.version)
//    if (snapshot.version > 0) {
//      httpResp.headers().add("Content-Type", "application/json")
//      httpResp.end(snapshotJson.encode())
//    } else {
//      httpResp.setStatusCode(404).end("Entity not found")
//    }
//
//  })
//
//}
//
//fun <E : Entity> entityTrackingHandler(rc: RoutingContext, entityId: Int, component: EntityComponent<E>,
//                                       entityToJson: (E) -> JsonObject) {
//
//  log.info("Retrieving entity tracking for $entityId")
//
//  val httpResp = rc.response()
//
//  val snapshotFuture = Future.future<Snapshot<E>>()
//  component.getSnapshot(entityId, snapshotFuture)
//
//  val uowListFuture = Future.future<List<UnitOfWork>>()
//  component.getAllUowByEntityId(entityId, uowListFuture)
//
//  CompositeFuture.all(snapshotFuture, uowListFuture).setHandler { event ->
//    if (event.failed()) {
//      httpResp.setStatusCode(500).end("Server error")
//      return@setHandler
//    }
//
//    val result = JsonObject()
//    val snapshot = snapshotFuture.result()
//    val snapshotJson = JsonObject().put("state", entityToJson.invoke(snapshot.state)).put("version", snapshot.version)
//    if (snapshot.version > 0) result.put("snapshot", snapshotJson)
//    val uowList = uowListFuture.result()
//    if (uowList.isNotEmpty()) result.put("units_of_work", JsonArray(uowListFuture.result()))
//    if (result.isEmpty) {
//      httpResp.setStatusCode(404).end("Entity not found")
//    } else {
//      httpResp.setStatusCode(200).isChunked = true
//      httpResp.headers().add("Content-Type", "application/json")
//      httpResp.end(result.encode())
//    }
//  }
//
//}
