package io.github.crabzilla.core

import io.vertx.core.Future
import java.util.UUID

interface SnapshotRepository<E : Entity> {
  fun retrieve(entityId: Int): Future<Snapshot<E>>
  fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void>
}

interface UnitOfWorkJournal {
  fun append(unitOfWork: UnitOfWork): Future<Long>
}

interface UnitOfWorkRepository {
  fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork>
  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String): Future<RangeOfEvents>
  fun selectAfterUowId(uowId: Long, maxRows: Int): Future<List<UnitOfWorkEvents>>
  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>>
}
