package io.github.crabzilla.internal

import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.Future

interface UnitOfWorkJournal {

  fun append(unitOfWork: UnitOfWork) : Future<Long>
}
