package io.github.crabzilla

import java.util.*

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val targetId: Int, val events: List<DomainEvent>) {

  companion object {

    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {
      return ProjectionData(UUID.randomUUID(), uowSequence, uow.targetId().value(), uow.events)
    }

  }

}
