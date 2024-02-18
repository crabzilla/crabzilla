package io.crabzilla.stream

import io.crabzilla.context.CrabzillaRuntimeException
import io.crabzilla.context.EventRecord
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

data class TargetStream(
  private val stateType: String? = null,
  private val stateId: String? = null,
  val name: String = "$stateType@$stateId",
  val mustBeNew: Boolean = false,
) {
  fun stateType(): String {
    return stateType ?: name.split("@")[0]
  }

  fun stateId(): String {
    return stateId ?: name.split("@")[1]
  }
}

interface StreamRepository<S : Any> {
  fun getStreamId(): Future<Int>

  fun getSnapshot(streamId: Int): Future<StreamSnapshot<S>>

  companion object {
    const val NO_STREAM = -1
    const val QUERY_MAX_STREAM_SIZE = 1000
  }
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
