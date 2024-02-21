package io.github.crabzilla.stream

import io.github.crabzilla.context.EventRecord
import io.vertx.core.Future

// TODO WIP consider to wrap stream api as a verticle to be manageable like subscription

interface StreamWriter<S : Any, E : Any> {
  fun lockTargetStream(lockType: StreamWriterLockEnum = StreamWriterLockEnum.PG_ADVISORY_LOCKS): Future<Int>

  fun appendEvents(
    streamSnapshot: StreamSnapshot<S>,
    events: List<E>,
  ): Future<List<EventRecord>>
}
