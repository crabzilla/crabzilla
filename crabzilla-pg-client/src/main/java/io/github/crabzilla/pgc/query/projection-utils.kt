package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.EventBusChannels
import io.github.crabzilla.core.command.UnitOfWork
import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcStreamProjector
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

fun startProjectionConsumingFromEventbus(
  entityName: String,
  streamId: String,
  readContext: PgcReadContext,
  projectorDomain: PgcDomainEventProjector
) {
  fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
    val uowId = jsonObject.getLong("uowId")
    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
    val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsString)
    return UnitOfWorkEvents(uowId, entityId, events)
  }
  // TODO add startup behaviour like startProjectionConsumingFromDatabase does
  val channel = EventBusChannels.aggregateRootChannel(entityName)
  log.info("adding projector for $streamId subscribing on $channel")
  val (vertx, json, readDb) = readContext
  val uolProjector = PgcUnitOfWorkProjector(readDb, entityName, streamId, projectorDomain)
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

fun startProjectionConsumingFromDatabase(
  vertx: Vertx,
  projectionsRepo: PgcProjectionsRepo,
  streamProjector: PgcStreamProjector
): Future<Void> {
  val promise0 = Promise.promise<Void>()
  val isRunning = AtomicBoolean(false)
  val entityName = streamProjector.entityName()
  val streamId = streamProjector.streamId()
  fun react(): Future<Pair<Pair<Throwable, Long>?, Int>> {
    val promise = Promise.promise<Pair<Pair<Throwable, Long>?, Int>>()
    isRunning.set(true)
    projectionsRepo.selectLastUowId(entityName, streamId)
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
  // TODO signalize command handler with dependency to this stream in order to avoid commands until it's done
  projectionsRepo.selectLastUowId(entityName, streamId)
    .onFailure { err ->
      promise0.fail(err)
      log.error("On projectionRepo.selectLastUowId for entity $entityName streamId $streamId", err) }
    .onSuccess { lastStreamUow1 ->
      streamProjector.handle(lastStreamUow1, 100, entityName, Int.MAX_VALUE)
        .onFailure { err ->
          log.error("On streamProjector.handle for entity $entityName streamId $streamId ", err) }
        .onSuccess { startupResult: Pair<Pair<Throwable, Long>?, Int> ->
          log.info("${startupResult.second} units of work successfully projected on startup phase")
          val startupError = startupResult.first
          if (startupError != null) {
            log.error("There was an error with uowId ${startupError.second}. Will retry latter", startupError.first)
            // TODO retry later means query model is not fully updated, should we fail new commands until
            //  it's the stream became fully updated?
          }
          // after projecting missing events, react to repeat the operation given any event on this entityName
          val channel = EventBusChannels.aggregateRootChannel(entityName)
          log.info("db pooling projector for entity $entityName streamId $streamId is now ready")
          vertx.eventBus().consumer<Void>(channel) {
            log.info("Got message")
            if (isRunning.get()) {
              log.info("StreamProjector is already running. Skipping for now.")
              return@consumer
            }
            // TODO implement some backoff policy here perhaps using CircuitBreaker (triggered on first fail)
            // TODO or a rx Flow able
            react()
              .onSuccess { reactResult: Pair<Pair<Throwable, Long>?, Int> ->
                log.info("${reactResult.second} units of work successfully projected on startup phase")
                val reactError = reactResult.first
                if (reactError != null) {
                  log.error("There was an error with uowId ${reactError.second}. Will retry latter", reactError.first)
                }
              }
              .onFailure { err ->
                log.error("when trying to project event", err) }
          }
        }
      promise0.complete()
    }
  return promise0.future()
}
