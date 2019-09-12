package io.github.crabzilla.internal

import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.Promise

interface UnitOfWorkJournal<E: Entity> {

  fun append(unitOfWork: UnitOfWork) : Promise<Long>
}
