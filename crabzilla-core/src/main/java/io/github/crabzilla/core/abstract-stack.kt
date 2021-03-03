package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.UUID

interface UnitOfWorkJournal {
  fun append(unitOfWork: UnitOfWork): Future<Void>
}

interface SnapshotRepository<A : AggregateRoot> {
  fun retrieve(id: Int): Future<Snapshot<A>>
  fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void>
}

interface EventsRepository {
  fun getByAggregate(aggregateRootName: String, id: Int, since: Version): Future<Pair<List<DomainEvent>, Version>>
  fun getByCommand(id: UUID): Future<Pair<List<DomainEvent>, Version>>
}

interface CommandsRepository {
  fun getByCommand(id: UUID): Future<JsonObject>
}
