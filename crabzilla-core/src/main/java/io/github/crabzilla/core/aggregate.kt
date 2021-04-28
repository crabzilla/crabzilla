package io.github.crabzilla.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class DomainEvent

@Serializable
abstract class Command

@Serializable
abstract class AggregateRoot

// @Serializable
// abstract class ProcessManager

/**
 * To validate a command
 */
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
 * A configuration for an aggregate root
 */
class AggregateRootConfig<A : AggregateRoot, C : Command, E : DomainEvent> (
  val name: AggregateRootName,
  val snapshotTableName: SnapshotTableName,
  val eventHandler: EventHandler<A, E>,
  val commandValidator: CommandValidator<C>,
  val commandHandler: CommandHandler<A, C, E>,
  val json: Json
)

inline class AggregateRootName(val value: String) {
  init {
    if (value.length > 16) throw IllegalArgumentException("Aggregate root names can be at most 16 characters")
  }
}
inline class SnapshotTableName(val value: String)
