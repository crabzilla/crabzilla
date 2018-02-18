package io.github.crabzilla.core

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.beans.ConstructorProperties
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

data class Version @ConstructorProperties("valueAsLong") constructor(val valueAsLong: Long) {

  init {
    if (valueAsLong < 0) throw IllegalArgumentException("Version must be = zero or positive")
  }

  fun nextVersion(): Version {
    return Version(valueAsLong + 1)
  }

}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface EntityId : Serializable {
  fun stringValue(): String
}

interface EntityCommand : Command {
  val targetId: EntityId
}

data class UnitOfWork(val unitOfWorkId: UUID, val command: EntityCommand,
                      val version: Version, val events: List<DomainEvent>)  {

  fun targetId(): EntityId {
    return command.targetId
  }
}
