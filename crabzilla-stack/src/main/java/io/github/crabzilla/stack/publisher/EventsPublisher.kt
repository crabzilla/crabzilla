package io.github.crabzilla.stack.publisher

import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future

/**
 * To publish an event as JSON to read model, messaging broker, etc (any side effect)
 */
interface EventsPublisher {
  fun publish(eventRecord: EventRecord): Future<Void>
}
