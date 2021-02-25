package io.github.crabzilla.core.command

import io.vertx.core.Future
import java.util.UUID

interface SnapshotRepository<A : AggregateRoot> {
  fun retrieve(id: Int): Future<Snapshot<A>>
  fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void>
}

interface UnitOfWorkJournal {
  fun append(unitOfWork: UnitOfWork): Future<Void>
}

interface UnitOfWorkRepository {
  fun selectLastUowId(): Future<Long>
  fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>?>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork?>
  fun selectByAggregateRootId(id: Int): Future<List<UnitOfWork>>
}
