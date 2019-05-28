package io.github.crabzilla.internal

import io.github.crabzilla.RangeOfEvents
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.UnitOfWorkEvents
import io.github.crabzilla.Version
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String,
                         aHandler: Handler<AsyncResult<RangeOfEvents>>)

  fun selectAfterUowId(uowId: Long, maxRows: Int,
                       aHandler: Handler<AsyncResult<List<UnitOfWorkEvents>>>)

  fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>)
}
