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
