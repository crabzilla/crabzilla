package io.github.crabzilla.stack

import io.vertx.core.Future

interface EventsScanner {
  fun streamName(): String
  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>>
  fun updateOffSet(eventId: Long): Future<Void>
}
