package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.UUID

interface Repository<A : AggregateRoot> {
  // fun retrieve(id: Int): Future<A?>
  fun append(aggregate: A, expectedVersion: Int, command: Command, commandId: UUID = UUID.randomUUID()): Future<Void>
}

interface EventsRepository {
  fun getByAggregate(aggregateRootName: String, id: Int, since: Int): Future<Pair<List<DomainEvent>, Int>>
  fun getByCommand(id: UUID): Future<Pair<List<DomainEvent>, Int>>
}

interface CommandsRepository {
  fun getByCommand(id: UUID): Future<JsonObject>
}
