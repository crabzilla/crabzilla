package io.github.crabzilla.core.entity

import io.github.crabzilla.core.DomainEvent
import java.io.Serializable
import java.util.*

data class EntityUnitOfWork(val unitOfWorkId: UUID, val command: EntityCommand,
                       val version: Version, val events: List<DomainEvent>) : Serializable {

  fun targetId(): EntityId {
    return command.targetId
  }
}
