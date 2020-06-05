package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.EVENT_SERIALIZER
import io.github.crabzilla.core.command.EventBusChannels
import io.github.crabzilla.core.command.UnitOfWork
import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.github.crabzilla.pgc.command.PgcStreamProjector
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.atomic.AtomicBoolean

internal val log = getLogger("startProjection")

typealias PgcReadContext = Triple<Vertx, Json, PgPool>

fun startProjection(
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

  val channel = EventBusChannels.entityChannel(entityName)
  log.info("adding projector for $streamId subscribing on $channel")
  val (vertx, json, readDb) = readContext
  val uolProjector = PgcUowProjector(readDb, entityName, streamId, projector)
  vertx.eventBus().consumer<JsonObject>(channel) { message ->
    val uowEvents = toUnitOfWorkEvents(message.body(), json)
    log.info("Got $uowEvents")
    uolProjector.handle(uowEvents).onComplete { result ->
      if (result.failed()) {
        log.error("Projection for $streamId failed: " + result.cause().message)
      }
    }
  }
}

fun startProjection(
  vertx: Vertx,
  projectionRepo: PgcProjectionRepo,
  streamProjector: PgcStreamProjector
): Future<Void> {
  val promise0 = Promise.promise<Void>()
  val isRunning = AtomicBoolean(false)
  val entityName = streamProjector.entityName()
  val streamId = streamProjector.streamId()
  fun react(): Future<Long> {
    val promise = Promise.promise<Long>()
    isRunning.set(true)
    projectionRepo.selectLastUowId(entityName, streamId)
      .onFailure { err ->
        isRunning.set(false)
        promise.fail(err)
        log.error("On projectionRepo.selectLastUowId for entity $entityName streamId $streamId", err)
      }
      .onSuccess { lastStreamUow1 ->
        streamProjector.handle(lastStreamUow1, 10, entityName, 10)
          .onFailure { err ->
            isRunning.set(false)
            promise.fail(err)
            log.error("On streamProjector.handle for entity $entityName streamId $streamId ", err)
          }
          .onSuccess { rows2 ->
            isRunning.set(false)
            promise.complete(rows2)
            log.info("$rows2 units of work successfully projected")
          }
      }
    return promise.future()
  }
  log.info("starting db pooling projector for entity $entityName streamId $streamId")
  // on startup, get latest projected uowId
  // TODO sinalize command handler with dependency to this stream in order to avoid commands until it's done
  projectionRepo.selectLastUowId(entityName, streamId)
    .onFailure { err ->
      promise0.fail(err)
      log.error("On projectionRepo.selectLastUowId for entity $entityName streamId $streamId", err) }
    .onSuccess { lastStreamUow1 ->
      streamProjector.handle(lastStreamUow1, 100, entityName, Int.MAX_VALUE)
        .onFailure { err ->
//          promise0.fail(err) // TODO no need to wait?
          log.error("On streamProjector.handle for entity $entityName streamId $streamId ", err) }
        .onSuccess { rows ->
          log.info("$rows units of work successfully projected on startup phase")
          // after projecting missing events, react to repeat the operation given any event on this entityName
          val channel = EventBusChannels.entityChannel(entityName)
          log.info("db pooling projector for entity $entityName streamId $streamId is now ready")
          vertx.eventBus().consumer<JsonObject>(channel) {
            log.info("Got message")
            if (isRunning.get()) {
              log.info("StreamProjector is already running. Skipping for now.")
              return@consumer
            }
            // TODO implement some backoff policy here (perhaps using CircuitBreaker (triggered on first fail)
            react()
              .onSuccess { rows ->
                log.info("$rows were projected on react phase") }
              .onFailure { err ->
                log.error("when trying to project event", err) }
          }
//          promise0.fail(err) // TODO no need to wait?
        }
      promise0.complete()
    }
  return promise0.future()
}
