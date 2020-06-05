package io.github.crabzilla.core.command

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.UUID

interface SnapshotRepository<E : Entity> {
  fun retrieve(entityId: Int): Future<Snapshot<E>>
  fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void>
}

interface UnitOfWorkPublisher {
  fun publish(events: JsonObject)
}

interface UnitOfWorkJournal {
  fun append(unitOfWork: UnitOfWork): Future<Long>
}

interface UnitOfWorkRepository {
  fun selectLastUowId(): Future<Long>
  fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>?>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork?>
  fun selectByEntityId(id: Int): Future<List<UnitOfWork>>
}
