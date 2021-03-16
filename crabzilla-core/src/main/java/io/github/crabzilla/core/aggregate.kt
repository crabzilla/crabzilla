package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.serialization.Serializable

@Serializable
abstract class DomainEvent

@Serializable
abstract class Command

@Serializable
abstract class AggregateRoot

// @Serializable
// abstract class ProcessManager

fun interface CommandValidator<C : Command> {
  fun validate(command: C): List<String>
}

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<A : AggregateRoot, E : DomainEvent> {
  fun handleEvent(state: A?, event: E): A
}

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<A : AggregateRoot>(val state: A, val version: Int)

/**
 * To handle commands
 */
interface CommandHandler<A : AggregateRoot, C : Command, E : DomainEvent> {

  class ConstructorResult<A, E>(val state: A, vararg val events: E)

  fun <A : AggregateRoot, E : DomainEvent> with(create: ConstructorResult<A, E>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
      return StatefulSession(create, applier)
    }

  fun <A : AggregateRoot, E : DomainEvent> with(snapshot: Snapshot<A>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
      return StatefulSession(snapshot.version, snapshot.state, applier)
    }

  fun handleCommand(command: C, snapshot: Snapshot<A>?): Result<StatefulSession<A, E>>
}

/**
 * To serialize events into plain JSON (integration events)
 */
interface EventSerializer<E : Any> {
  fun toJson(e: E): Result<JsonObject>
}

/**
 * To deserialize integration events from upstream services
 */
interface EventDeserializer<E : DomainEvent> { // sagas / process managers will need it
  fun fromJson(type: String, j: JsonObject): Result<E>
}

/**
 * To publish an event to read model, messaging broker, etc (any side effect)
 */
interface EventPublisher<E : DomainEvent> {
  fun project(id: Int, event: E): Future<Void>
}