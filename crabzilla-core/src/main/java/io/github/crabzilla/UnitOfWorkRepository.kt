package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, future: Future<UnitOfWork>)

  fun getUowByUowId(uowId: UUID, future: Future<UnitOfWork>)

  operator fun get(query: String, id: UUID, future: Future<UnitOfWork>)

  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String, aHandler: Handler<AsyncResult<SnapshotData>>)

  fun append(unitOfWork: UnitOfWork, aggregateRootName: String, aHandler: Handler<AsyncResult<Int>>)

  fun selectAfterUowSequence(uowSequence: Int, maxRows: Int, future: Future<List<ProjectionData>>)

}
