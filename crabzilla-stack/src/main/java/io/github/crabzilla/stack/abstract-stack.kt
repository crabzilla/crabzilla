package io.github.crabzilla.stack

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.UUID
import java.util.function.BiFunction

// es/cqrs infra stack

/**
 * A configuration for an aggregate root
 */
class AggregateRootConfig<A : AggregateRoot, C : Command, E : DomainEvent> (
  val name: String,
  val eventHandler: EventHandler<A, E>,
  val commandValidator: CommandValidator<C>,
  val commandHandler: CommandHandler<A, C, E>
)

@JvmInline
value class AggregateRootId(val id: UUID)

@JvmInline
value class CommandId(val id: UUID = UUID.randomUUID())

@JvmInline
value class EventId(val id: UUID)

@JvmInline
value class CorrelationId(val id: UUID)

@JvmInline
value class CausationId(val id: UUID)

data class CommandMetadata(
  val aggregateRootId: AggregateRootId,
  val commandId: CommandId = CommandId(UUID.randomUUID()),
  val correlationId: CorrelationId = CorrelationId(commandId.id),
  val causationId: CausationId = CausationId(commandId.id)
) {
  companion object {
    fun fromJson(json: JsonObject): CommandMetadata {
      return CommandMetadata(
        AggregateRootId(UUID.fromString(json.getString("aggregateRootId"))),
        CommandId(UUID.fromString(json.getString("commandId"))),
        CorrelationId(UUID.fromString(json.getString("correlationId"))),
        CausationId(UUID.fromString(json.getString("causationId")))
      )
    }
  }
  fun toJson(): JsonObject {
    return JsonObject()
      .put("aggregateRootId", aggregateRootId.id.toString())
      .put("commandId", commandId.id.toString())
      .put("correlationId", correlationId.id.toString())
      .put("causationId", causationId.id.toString())
  }
}

data class EventMetadata(
  val aggregateName: String,
  val aggregateRootId: AggregateRootId,
  val eventId: EventId,
  val correlationId: CorrelationId,
  val causationId: CausationId,
  val eventSequence: Long
)

/**
 * An event store to append new events
 */
interface EventStore<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void>
}

/**
 * A repository for snapshots
 */
interface SnapshotRepository<A : AggregateRoot> {
  fun get(id: UUID): Future<Snapshot<A>?>
}

/**
 * To scan for new events
 */
interface EventsScanner {
  fun streamName(): String
  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>>
  fun updateOffSet(eventSequence: Long): Future<Void>
}

/**
 * To publish an event as JSON to read model, messaging broker, etc (any side effect)
 */
interface EventsPublisher {
  fun publish(eventRecord: EventRecord): Future<Void>
}

/**
 * A helper
 */
fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
  var result = identity
  while (iterator.hasNext()) {
    val next = iterator.next()
    result = bf.apply(result, next)
  }
  return result
}

/**
 * Deploy verticles
 */
fun Vertx.deployVerticles(
  verticles: List<String>,
  opt: DeploymentOptions = DeploymentOptions().setInstances(1)
): Future<Void> {
  val promise = Promise.promise<Void>()
  val initialFuture = Future.succeededFuture<String>()
  foldLeft(
    verticles.iterator(),
    initialFuture,
    { currentFuture: Future<String>, verticle: String ->
      currentFuture.compose {
        deployVerticle(verticle, opt)
      }
    }
  ).onComplete {
    if (it.failed()) {
      promise.fail(it.cause())
    } else {
      promise.complete()
    }
  }
  return promise.future()
}
