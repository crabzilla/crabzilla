package io.github.crabzilla.hazelcast.query

import io.vertx.core.Future

sealed class Either<out L, out R>
class Left<out L>(value: L) : Either<L, Nothing>()
class Right<out R>(value: R) : Either<Nothing, R>()

interface HzProjectionHandler {
  fun handle(sequence: Long, eventMessage: DomainEventMessage): Future<Either<Int, String>>
}
