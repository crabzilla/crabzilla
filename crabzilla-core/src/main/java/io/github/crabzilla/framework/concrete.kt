package io.github.crabzilla.framework

import kotlinx.serialization.PolymorphicSerializer
import java.util.*

val ENTITY_SERIALIZER = PolymorphicSerializer(Entity::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)
val EVENT_SERIALIZER = PolymorphicSerializer(DomainEvent::class)

typealias Version = Int

data class CommandMetadata(val entityId: Int, val commandName: String, val commandId: UUID = UUID.randomUUID())

data class Snapshot<E : Entity>(val state: E, val version: Version)

class StateTransitionsTracker<A : Entity>(originalSnapshot: Snapshot<A>, private val stateFn: (DomainEvent, A) -> A) {
  val appliedEvents = mutableListOf<DomainEvent>()
  var currentState: A = originalSnapshot.state
  fun applyEvents(events: List<DomainEvent>): StateTransitionsTracker<A> {
    events.forEach { domainEvent ->
      currentState = stateFn.invoke(domainEvent, currentState)
      appliedEvents.add(domainEvent)
    }
    return this
  }
  inline fun applyEvents(fn: (A) -> List<DomainEvent>): StateTransitionsTracker<A> {
    val newEvents = fn.invoke(currentState)
    return applyEvents(newEvents)
  }
}

data class UnitOfWork(val entityName: String, val entityId: Int, val commandId: UUID, val command: Command,
                      val version: Version, val events: List<DomainEvent>) {
  init { require(this.version >= 1) { "version must be >= 1" } }
  companion object {
    @JvmStatic
    fun of(entityId: Int, entityName: String, commandId: UUID, command: Command,
           events: List<DomainEvent>, resultingVersion: Version): UnitOfWork {
      return UnitOfWork(entityName, entityId, commandId, command, resultingVersion, events)
    }
  }
  object JsonMetadata {
    const val ENTITY_NAME = "entityName"
    const val ENTITY_ID = "entityId"
    const val COMMAND_ID = "commandId"
    const val COMMAND = "command"
    const val VERSION = "version"
    const val EVENTS = "events"
  }
}
