package io.github.crabzilla

import java.util.*

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val entityId: Int,
                          val events: List<Pair<String, DomainEvent>>) {
  companion object {
    @JvmStatic
    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {
      return ProjectionData(uow.unitOfWorkId, uowSequence, uow.entityId, uow.events)
    }
  }
}
