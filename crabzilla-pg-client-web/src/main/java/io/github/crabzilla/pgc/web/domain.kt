package io.github.crabzilla.pgc.web

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.internal.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.PgcEventBusChannels
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.pgclient.PgPool
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("web-pgc-domain")

typealias WebPgcReadContext = Triple<Vertx, Json, PgPool>

typealias WebPgcWriteContext = Triple<Vertx, Json, PgPool>

class ResourceContext<E : Entity>(
  val resourceName: String,
  val entityName: String,
  val cmdAware: EntityCommandAware<E>,
  val cmdTypeMap: Map<String, String>
)

fun <E : Entity> addResourceForEntity(writeContext: WebPgcWriteContext, rsContext: ResourceContext<E>, router: Router) {
  val (vertx, json, writeDb) = writeContext
  log.info("adding web command handler for entity $rsContext.entityName on resource $rsContext.resourceName")
  val cmdHandlerComponent = PgcEntityComponent(vertx, writeDb, rsContext.cmdAware, json, rsContext.entityName)
  WebDeployer(rsContext.resourceName, rsContext.cmdTypeMap, cmdHandlerComponent, router).deployWebRoutes()
}

fun addProjector(readContext: WebPgcReadContext, projectionName: String, projector: PgcEventProjector) {
  log.info("adding projector for $projectionName subscribing on ${PgcEventBusChannels.unitOfWorkChannel}")
  val (vertx, json, readDb) = readContext
  vertx.eventBus().consumer<JsonObject>(PgcEventBusChannels.unitOfWorkChannel) { message ->
    val uowEvents = toUnitOfWorkEvents(message.body(), json)
    val uolProjector = PgcUowProjector(readDb, projectionName)
    uolProjector.handle(uowEvents, projector).onComplete { result ->
      if (result.failed()) { // TODO circuit breaker
        log.error("Projection [$projectionName] failed: " + result.cause().message)
      }
    }
  }
}

private fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
  val uowId = jsonObject.getLong("uowId")
  val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
  val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
  val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsString)
  return UnitOfWorkEvents(uowId, entityId, events)
}
