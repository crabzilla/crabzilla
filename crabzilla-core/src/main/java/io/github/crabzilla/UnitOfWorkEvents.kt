package io.github.crabzilla

import java.math.BigInteger

data class UnitOfWorkEvents(val uowSequence: BigInteger, val entityId: Int,
                            val events: List<Pair<String, DomainEvent>>) {
  companion object {
    @JvmStatic
    fun fromUnitOfWork(uowSequence: BigInteger, uow: UnitOfWork) : UnitOfWorkEvents {
      return UnitOfWorkEvents(uowSequence, uow.entityId, uow.events)
    }
  }
}
