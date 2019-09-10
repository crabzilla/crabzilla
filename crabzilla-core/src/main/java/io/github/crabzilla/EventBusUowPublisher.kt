package io.github.crabzilla

import io.github.crabzilla.framework.UnitOfWork.JsonMetadata
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityJsonAware
import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class EventBusUowPublisher(val vertx: Vertx,
                           private val jsonFunctions: Map<String, EntityJsonAware<out Entity>>)
  : UnitOfWorkPublisher {

  internal val log = LoggerFactory.getLogger(EventBusUowPublisher::class.java)

  override fun publish(uow: UnitOfWork, uowId: Long, handler: Handler<AsyncResult<Void>>) {
    val jsonAware = jsonFunctions[uow.entityName]
    log.info("jsonAware $jsonAware")
    if (jsonAware == null) {
      handler.handle(Future.failedFuture("JsonAware for $uow.entityName wasn't found"))
    } else {
      val eventsAsJson: JsonArray = jsonAware.toJsonArray(uow.events)
      val message = JsonObject()
        .put("uowId", uowId)
        .put(JsonMetadata.ENTITY_NAME, uow.entityName)
        .put(JsonMetadata.ENTITY_ID, uow.entityId)
        .put(JsonMetadata.VERSION, uow.version)
        .put(JsonMetadata.EVENTS, eventsAsJson)
      log.info("will publish message $message")
      vertx.eventBus().publish(EventBusChannels.unitOfWorkChannel, message.encode())
      log.info("publish success")
      handler.handle(Future.succeededFuture())
    }
  }

}
