package io.github.crabzilla.internal

import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.framework.Version
import io.vertx.core.Future
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID) : Future<Pair<UnitOfWork, Long>>

  fun getUowByUowId(uowId: Long) : Future<UnitOfWork>

  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String): Future<RangeOfEvents>

  fun selectAfterUowId(uowId: Long, maxRows: Int): Future<List<UnitOfWorkEvents>>

  fun getAllUowByEntityId(id: Int) : Future<List<UnitOfWork>>
}
