package io.github.crabzilla.stack.storage

import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future

/**
 * To scan for new events
 */
interface EventsScanner {
  fun streamName(): String
  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>>
  fun updateOffSet(eventSequence: Long): Future<Void>
}
