package io.github.crabzilla.core

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface DomainEvent : Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface EntityId : Serializable {
  fun stringValue(): String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface Command : Serializable {
  val commandId: UUID
  val targetId: EntityId
}

typealias Version = Long

data class UnitOfWork(val unitOfWorkId: UUID, val command: Command,
                      val version: Version, val events: List<DomainEvent>) : Serializable  {
  init {
    require(this.version >= 0, { "version must be >= 0" })
  }

  fun targetId(): EntityId {
    return command.targetId
  }
}
