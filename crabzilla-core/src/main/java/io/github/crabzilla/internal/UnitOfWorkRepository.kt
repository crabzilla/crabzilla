package io.github.crabzilla.internal

import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.framework.Version
import io.vertx.core.Promise
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID) : Promise<Pair<UnitOfWork, Long>>

  fun getUowByUowId(uowId: Long) : Promise<UnitOfWork>

  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String): Promise<RangeOfEvents>

  fun selectAfterUowId(uowId: Long, maxRows: Int): Promise<List<UnitOfWorkEvents>>

  fun getAllUowByEntityId(id: Int) : Promise<List<UnitOfWork>>
}
