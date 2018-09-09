package io.github.crabzilla.vertx

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.UnitOfWork
import java.util.*

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val targetId: Int, val events: List<DomainEvent>) {

  companion object {

    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {
      return ProjectionData(UUID.randomUUID(), uowSequence, uow.targetId().value(), uow.events)
    }

  }

}
