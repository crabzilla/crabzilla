package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.EventBusChannels
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkEvents
import io.github.crabzilla.pgc.command.PgcUowRepo
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory.getLogger

internal val log = getLogger("addProjector")

typealias PgcReadContext = Triple<Vertx, Json, PgPool>

fun addEventBusProjector(
  entityName: String,
  streamId: String,
  readContext: PgcReadContext,
  projector: PgcEventProjector
) {
  fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
    val uowId = jsonObject.getLong("uowId")
    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
    val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsString)
    return UnitOfWorkEvents(uowId, entityId, events)
  }
  log.info("adding projector for $streamId subscribing on ${EventBusChannels.unitOfWorkChannel}")
  val (vertx, json, readDb) = readContext
  val uolProjector = PgcUowProjector(readDb, entityName, streamId, projector)
  vertx.eventBus().consumer<JsonObject>(EventBusChannels.unitOfWorkChannel) { message ->
    val uowEvents = toUnitOfWorkEvents(message.body(), json)
    uolProjector.handle(uowEvents).onComplete { result ->
      if (result.failed()) {
        log.error("Projection [$streamId] failed: " + result.cause().message)
      }
    }
  }
}

fun addDbPoolingProjector(
  entityName: String,
  streamId: String,
  readContext: PgcReadContext,
  projector: PgcEventProjector,
  uowRepo: PgcUowRepo,
  projectionRepo: PgcProjectionRepo
) {

  log.info("adding db pooling projector for $streamId")
  val (vertx, _, readDb) = readContext
  val uolProjector = PgcUowProjector(readDb, entityName, streamId, projector)
  // on startup, get latest projected uowId
  projectionRepo.selectLastUowId(entityName, streamId)
    .onFailure { err -> log.error("On projectionRepo.selectLastUowId for stream $streamId ", err) }
    .onSuccess { lastStreamUow ->
      // then retrieve missing events from entityName
      uowRepo.selectAfterUowId(lastStreamUow, Int.MAX_VALUE, entityName)
        .onFailure { err -> log.error("On uowRepo.selectAfterUowId for stream $streamId ", err) }
        .onSuccess { uowEventsList: List<UnitOfWorkEvents> ->
          // then transactional project each unit of work
          log.info("Projecting {} events for stream {}", uowEventsList.size, streamId)
          val toFuture: (Int) -> () -> Future<Void> = { index -> { uolProjector.handle(uowEventsList[index]) } }
          val futures: List<() -> Future<Void>> = List(uowEventsList.size, toFuture)
          futures.fold(Future.succeededFuture()) { previousFuture: Future<Void>,
              currentFuture: () -> Future<Void> ->
              previousFuture.compose { currentFuture.invoke() }
            }
            .onFailure { err -> log.error("On handling projection for stream $streamId ", err) }
            .onSuccess {
              // after projecting missing events, repeat the operation given any event on this entityName
              if (log.isDebugEnabled) {
                log.debug("${uowEventsList.size} events projected successfully on stream $streamId")
              }
              vertx.eventBus().consumer<Void>(entityName) {
                uowRepo.selectAfterUowId(lastStreamUow, 100, entityName)
                  .onFailure { err -> log.error("On uowRepo.selectAfterUowId for stream $streamId ", err) }
                  .onSuccess { uowEventsList: List<UnitOfWorkEvents> ->
                    log.info("Projecting {} events for stream {}", uowEventsList.size, streamId)
                    val toFuture1: (Int) -> () -> Future<Void> = { index -> { uolProjector.handle(uowEventsList[index]) } }
                    val futures1: List<() -> Future<Void>> = List(uowEventsList.size, toFuture1)
                    futures1.fold(Future.succeededFuture()) { previousFuture: Future<Void>,
                        currentFuture: () -> Future<Void> ->
                        previousFuture.compose { currentFuture.invoke() }
                      }
                      .onFailure { err -> log.error("On handling projection for stream $streamId ", err) }
                      .onSuccess {
                        if (log.isDebugEnabled) {
                          log.debug("${uowEventsList.size} events projected successfully on stream $streamId")
                        }
                      }
                  }
              }
            }
        }
    }
}
