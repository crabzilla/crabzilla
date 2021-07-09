package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class DomainEvent {
  companion object {
    private val serDer = PolymorphicSerializer(DomainEvent::class)
    fun <D : DomainEvent> fromJson(json: Json, asJson: String): D {
      return json.decodeFromString(serDer, asJson) as D
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}

@Serializable
abstract class Command {
  companion object {
    private val serDer = PolymorphicSerializer(Command::class)
    fun <C : Command> fromJson(json: Json, asJson: String): C {
      return json.decodeFromString(serDer, asJson) as C
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}

@Serializable
abstract class DomainState {
  companion object {
    private val serDer = PolymorphicSerializer(DomainState::class)
    fun <A : DomainState> fromJson(json: Json, asJson: String): A {
      return json.decodeFromString(serDer, asJson) as A
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}

/**
 * To validate a command
 */
fun interface CommandValidator<C : Command> {
  fun validate(command: C): List<String>
}

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<A : DomainState, E : DomainEvent> {
  fun handleEvent(state: A?, event: E): A
}

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<A : DomainState>(val state: A, val version: Int)

interface CommandHandlerApi<A : DomainState, C : Command, E : DomainEvent> {

  class ConstructorResult<A, E>(val state: A, vararg val events: E)

  fun <A : DomainState, E : DomainEvent> withNew(create: ConstructorResult<A, E>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(create, applier)
  }

  fun <A : DomainState, E : DomainEvent> with(snapshot: Snapshot<A>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(snapshot.version, snapshot.state, applier)
  }
}

/**
 * To handle commands
 */
interface CommandHandler<A : DomainState, C : Command, E : DomainEvent> : CommandHandlerApi<A, C, E> {

  fun handleCommand(command: C, eventHandler: EventHandler<A, E>, snapshot: Snapshot<A>?): StatefulSession<A, E>
}
