 package io.github.crabzilla.hazelcast.example1

// import io.github.crabzilla.hazelcast.example1.datamodel.tables.daos.CustomerSummaryDao
// import io.github.crabzilla.hazelcast.example1.datamodel.tables.pojos.CustomerSummary
// import io.github.crabzilla.hazelcast.query.DomainEventMessage
// import io.github.crabzilla.hazelcast.query.Either
// import io.github.crabzilla.hazelcast.query.HzProjectionHandler
// import io.github.crabzilla.hazelcast.query.Left
// import io.github.crabzilla.hazelcast.query.Right
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import io.vertx.core.shareddata.AsyncMap
// import org.slf4j.LoggerFactory
//
// class HzExample1ProjectionHandler(
//   private val hzProjectionsMap: AsyncMap<String, Long>,
//   private val streamId: String,
//   private val customerDao: CustomerSummaryDao
// ) : HzProjectionHandler {
//
//   companion object {
//     private val log = LoggerFactory.getLogger(HzExample1ProjectionHandler::class.java)
//   }
//
//  override fun handle(sequence: Long, eventMessage: DomainEventMessage): Future<Either<Int, String>> {
//    val (uowId, entityId, event) = eventMessage
//    val promise = Promise.promise<Either<Int, String>>()
//    try {
//      when (event) {
//        is CustomerCreated -> {
//          customerDao.insert(CustomerSummary(entityId, event.name, false))
//            .onFailure { err -> log.error("Event ${event::class.java.simpleName}", err)
//                                promise.complete(Left(entityId)) }
//        }
//        is CustomerActivated -> {
//          customerDao.findOneById(entityId)
//            .onSuccess { c ->
//              c.isActive = true
//              customerDao.update(c) }
//            .onFailure { err -> log.error("Event ${event::class.java.simpleName}", err)
//              promise.complete(Left(entityId)) }
//        }
//        is CustomerDeactivated -> {
//          customerDao.findOneById(entityId)
//            .onSuccess { c ->
//              c.isActive = false
//              customerDao.update(c) }
//            .onFailure { err -> log.error("Event ${event::class.java.simpleName}", err)
//              promise.complete(Left(entityId)) }
//        }
//      }
//      hzProjectionsMap.put("streamId", sequence) { result ->
//        if (result.failed()) {
//          log.error("Failed to put sequence $sequence on stream $streamId on map $hzProjectionsMap")
//        }
//      }
//      promise.complete(Right("ok"))
//    } catch (e: Exception) {
//      promise.complete(Left(entityId))
//    }
//    return promise.future()
//  }
// }
