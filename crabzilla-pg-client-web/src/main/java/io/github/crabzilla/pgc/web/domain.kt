package io.github.crabzilla.pgc.web

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.SnapshotRepository
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.PgcEventBusChannels
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcSnapshotRepo
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

typealias PgcReadContext = Triple<Vertx, Json, PgPool>

typealias PgcWriteContext = Triple<Vertx, Json, PgPool>

class WebResourceContext<E : Entity>(
  val resourceName: String,
  val entityName: String,
  val cmdAware: EntityCommandAware<E>,
  val cmdTypeMap: Map<String, String>
)

fun <E : Entity> addResourceForEntity(
  writeCtx: PgcWriteContext,
  webResourceCtx: WebResourceContext<E>,
  router: Router,
  snapshotRepo: SnapshotRepository<E> = PgcSnapshotRepo(writeCtx.third, writeCtx.second,
    webResourceCtx.entityName, webResourceCtx.cmdAware)
) {
  val (vertx, json, writeDb) = writeCtx
  log.info("adding web command handler for entity $webResourceCtx.entityName on resource $webResourceCtx.resourceName")
  val cmdHandlerComponent =
    PgcEntityComponent(vertx, writeDb, webResourceCtx.cmdAware, json, webResourceCtx.entityName, snapshotRepo)
  WebDeployer(webResourceCtx.resourceName, webResourceCtx.cmdTypeMap, cmdHandlerComponent, router).deployWebRoutes()
}

fun addProjector(readContext: PgcReadContext, streamId: String, projector: PgcEventProjector) {
  fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
    val uowId = jsonObject.getLong("uowId")
    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
    val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsString)
    return UnitOfWorkEvents(uowId, entityId, events)
  }
  log.info("adding projector for $streamId subscribing on ${PgcEventBusChannels.unitOfWorkChannel}")
  val (vertx, json, readDb) = readContext
  vertx.eventBus().consumer<JsonObject>(PgcEventBusChannels.unitOfWorkChannel) { message ->
    val uowEvents = toUnitOfWorkEvents(message.body(), json)
    val uolProjector = PgcUowProjector(readDb, streamId, projector)
    uolProjector.handle(uowEvents).onComplete { result ->
      if (result.failed()) { // TODO circuit breaker
        log.error("Projection [$streamId] failed: " + result.cause().message)
      }
    }
  }
}
