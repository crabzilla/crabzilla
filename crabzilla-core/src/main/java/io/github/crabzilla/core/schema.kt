package io.github.crabzilla.core

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable
import java.util.*


/**
 * A Command interface.
 *
 * The JsonTypeInfo annotation enables a polymorphic JSON serialization for your commands.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface Command : Serializable {

  /**
   * An unique ID for your command.
   */
  val commandId: UUID

}

/**
 * A DomainEvent interface.
 *
 * The JsonTypeInfo annotation enables a polymorphic JSON serialization for your events.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface DomainEvent : Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface EntityId : Serializable {
  fun stringValue(): String
}

interface EntityCommand : Command {
  val targetId: EntityId
}

typealias Version = Long

data class UnitOfWork(val unitOfWorkId: UUID, val command: EntityCommand,
                      val version: Version, val events: List<DomainEvent>)  {
  init {
    require(this.version >= 0, { "version must be >= 0" })
  }

  fun targetId(): EntityId {
    return command.targetId
  }
}
