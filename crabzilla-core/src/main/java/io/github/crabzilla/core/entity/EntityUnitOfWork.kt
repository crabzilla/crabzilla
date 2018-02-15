package io.github.crabzilla.core.entity

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import java.io.Serializable
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface UnitOfWork {
  val unitOfWorkId: UUID
  val command: Command
  val version: Version
  val events: List<DomainEvent>
  fun targetId(): EntityId
}

data class EntityUnitOfWork(override val unitOfWorkId: UUID, override val command: EntityCommand,
                            override val version: Version, override val events: List<DomainEvent>) : UnitOfWork {

  override fun targetId(): EntityId {
    return command.targetId
  }
}

data class ExternalServiceUnitOfWork(val id: EntityId, override val unitOfWorkId: UUID, override val command: Command,
                                     override val version: Version, override val events: List<DomainEvent>) : UnitOfWork {
  override fun targetId(): EntityId {
    return id
  }

}
