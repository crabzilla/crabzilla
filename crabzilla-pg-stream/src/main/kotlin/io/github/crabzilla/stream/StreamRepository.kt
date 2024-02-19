package io.github.crabzilla.stream

import io.vertx.core.Future

interface StreamRepository<S : Any> {
  fun getStreamId(): Future<Int>

  fun getSnapshot(
    streamId: Int,
    fromSnapshot: StreamSnapshot<S>? = null,
  ): Future<StreamSnapshot<S>>

  companion object {
    const val NO_STREAM = -1
    const val QUERY_MAX_STREAM_SIZE = 1000
  }
}
