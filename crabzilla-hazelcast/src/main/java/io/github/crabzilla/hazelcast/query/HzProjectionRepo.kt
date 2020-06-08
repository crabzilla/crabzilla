package io.github.crabzilla.hazelcast.query

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.shareddata.AsyncMap
import org.slf4j.LoggerFactory

class HzProjectionRepo(private val map: AsyncMap<String, Long>) {

  companion object {
    private val log = LoggerFactory.getLogger(HzProjectionRepo::class.java)
  }

  fun getLastSequence(entityName: String, streamId: String): Future<Long> {
    val promise = Promise.promise<Long>()
    map.get("$entityName-$streamId") { event2 ->
      if (event2.failed()) {
        log.error("Failed to get $streamId on map $entityName-$streamId")
        promise.fail(event2.cause())
        return@get
      }
      val result = event2.result()
      if (log.isDebugEnabled) {
        log.debug("Get last sequence for $streamId on map $entityName-$streamId: ${result ?: 0L}")
      }
      promise.complete(result ?: 0L)
    }
    return promise.future()
  }

  fun setLastSequence(entityName: String, streamId: String, sequence: Long): Future<Long> {
    val promise = Promise.promise<Long>()
    map.put("$entityName-$streamId", sequence) { event2 ->
      if (event2.failed()) {
        log.error("Failed to put $streamId on map $entityName-$streamId")
        promise.fail(event2.cause())
        return@put
      }
      if (log.isDebugEnabled) {
        log.debug("Last sequence for $streamId on map $entityName-$streamId updated to $sequence")
      }
      promise.complete()
    }
    return promise.future()
  }
}
