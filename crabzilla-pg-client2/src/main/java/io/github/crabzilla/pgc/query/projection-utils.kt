// package io.github.crabzilla.pgc.query
//
// import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
// import io.github.crabzilla.core.command.DomainEvent
// import io.github.crabzilla.core.command.EventBusChannels
// import io.github.crabzilla.core.command.UnitOfWork
// import io.github.crabzilla.core.command.UnitOfWorkEvents
// import io.github.crabzilla.pgc.PgcStreamProjector
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import io.vertx.core.Vertx
// import io.vertx.core.eventbus.MessageConsumer
// import io.vertx.core.json.JsonObject
// import io.vertx.pgclient.PgPool
// import kotlinx.serialization.builtins.list
// import kotlinx.serialization.json.Json
// import org.slf4j.LoggerFactory.getLogger
//
// internal val log = getLogger("startProjection")
//
// typealias PgcReadContext = Triple<Vertx, Json, PgPool>
//
// fun startStreamProjectionConsumer(
//  entityName: String,
//  streamId: String,
//  readContext: PgcReadContext,
//  projectorDomain: PgcDomainEventProjector
// ): MessageConsumer<JsonObject> {
//  fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
//    val uowId = jsonObject.getLong("uowId")
//    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
//    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
//    val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsString)
//    return UnitOfWorkEvents(uowId, entityId, events)
//  }
//  // TODO add startup behaviour like startProjectionConsumingFromDatabase does?
//  val channel = EventBusChannels.aggregateRootChannel(entityName)
//  log.info("adding projector subscribing on $channel")
//  val (vertx, json, readDb) = readContext
//  val uolProjector = PgcUnitOfWorkProjector(readDb, entityName, streamId, projectorDomain)
//  val consumer = vertx.eventBus().consumer<JsonObject>(channel)
//  consumer.handler { message ->
//    val uowEvents = toUnitOfWorkEvents(message.body(), json)
//    log.info("Got $uowEvents")
//    uolProjector.handle(uowEvents).onComplete { result ->
//      if (result.failed()) {
//        log.error("Projection for $channel failed: ", result.cause())
//      }
//    }
//  }
//  return consumer
// }
//
// fun startStreamProjectionDbPoolingProducer(
//  vertx: Vertx,
//  projectionsRepo: PgcProjectionsRepo,
//  streamProjector: PgcStreamProjector
// ): Future<Long> {
//  val promise0 = Promise.promise<Long>()
//  val entityName = streamProjector.entityName
//  val streamId = streamProjector.streamId
//  fun react(): Future<Int> {
//    val promise = Promise.promise<Int>()
//    projectionsRepo.selectLastUowId(entityName, streamId)
//      .onFailure { err ->
//        promise.fail(err)
//        log.error("On projectionRepo.selectLastUowId for entity $entityName streamId $streamId", err)
//      }
//      .onSuccess { lastStreamUow1 ->
//        // TODO stream projector must set selectLastUowId for each row
//        streamProjector.handle(lastStreamUow1, 100, entityName, 1000) // TODO Fix there magic numbers
//          .onFailure { err ->
//            promise.fail(err)
//            log.error("On streamProjector.handle for entity $entityName streamId $streamId ", err)
//          }
//          .onSuccess { rows2 ->
//            promise.complete(rows2)
//            log.info("$rows2 units of work successfully projected")
//          }
//      }
//    return promise.future()
//  }
//  log.info("starting db pooling projector for entity $entityName streamId $streamId")
//  // on startup, get latest projected uowId
//  // TODO signalize command handler with dependency to this stream in order to avoid commands until it's done
//  var timerId = 0L
//  projectionsRepo.selectLastUowId(entityName, streamId)
//    .onFailure { err ->
//      promise0.fail(err)
//      log.error("On projectionRepo.selectLastUowId for entity $entityName streamId $streamId", err)
//    }
//    .onSuccess { lastStreamUow1 ->
//      streamProjector.handle(lastStreamUow1, 100, entityName, Int.MAX_VALUE)
//        .onFailure { err ->
//          log.error("On streamProjector.handle for entity $entityName streamId $streamId ", err)
//        }
//        .onSuccess { startupResult: Int ->
//          log.info("$startupResult units of work successfully projected on startup phase")
//          timerId = vertx.setPeriodic(1000) { tick -> // TODO save timerId
//            react()
//              .onSuccess { reactResult: Int ->
//                log.info("$reactResult units of work successfully projected on startup phase")
//              }
//              .onFailure { err ->
//                log.error("when trying to project event", err)
//              }
//          }
//        }
//      promise0.complete(timerId)
//    }
//  return promise0.future()
// }
