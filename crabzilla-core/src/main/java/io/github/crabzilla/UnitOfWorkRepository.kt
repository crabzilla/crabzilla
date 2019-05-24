package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import java.math.BigInteger
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun getUowByUowId(uowId: BigInteger, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String,
                         aHandler: Handler<AsyncResult<RangeOfEvents>>)

  fun selectAfterUowId(uowId: BigInteger, maxRows: Int,
                       aHandler: Handler<AsyncResult<List<UnitOfWorkEvents>>>)

  fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>)
}
