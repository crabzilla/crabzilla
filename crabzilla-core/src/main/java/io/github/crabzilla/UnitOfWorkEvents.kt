package io.github.crabzilla

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int,
                            val events: List<Pair<String, DomainEvent>>) {
  companion object {
    @JvmStatic
    fun fromUnitOfWork(uowId: Long, uow: UnitOfWork) : UnitOfWorkEvents {
      return UnitOfWorkEvents(uowId, uow.entityId, uow.events)
    }
  }
}
