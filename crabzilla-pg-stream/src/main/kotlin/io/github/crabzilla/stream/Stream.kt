package io.github.crabzilla.stream

import io.github.crabzilla.context.CrabzillaRuntimeException
import io.github.crabzilla.context.EventRecord
import io.vertx.core.Future
import java.util.*

// TODO WIP consider to wrap stream api as a verticle to be manageable like subscription

data class StreamSnapshot<S : Any>(
  val streamId: Int,
  val state: S,
  val version: Int,
  val causationId: UUID?,
  val correlationId: UUID?,
)

interface StreamRepository<S : Any> {
  fun getStreamId(): Future<Int>

  fun getSnapshot(streamId: Int): Future<StreamSnapshot<S>>
}

interface StreamWriter<S : Any, E : Any> {
  fun lockTargetStream(): Future<Int>

  fun appendEvents(
    streamSnapshot: StreamSnapshot<S>,
    events: List<E>,
  ): Future<List<EventRecord>>
}

class StreamMustBeNewException(message: String) : CrabzillaRuntimeException(message)

class StreamCantBeLockedException(message: String) : CrabzillaRuntimeException(message)
