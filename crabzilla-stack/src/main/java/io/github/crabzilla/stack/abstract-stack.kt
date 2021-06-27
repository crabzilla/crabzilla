package io.github.crabzilla.stack

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.UUID

// es/cqrs infra stack

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
)

data class EventMetadata(
  val aggregateName: String,
  val aggregateRootId: AggregateRootId,
  val eventId: EventId,
  val correlationId: CorrelationId,
  val causationId: CausationId,
  val eventSequence: Long
)

class CrabzillaFactory(val vertx: Vertx) {
  fun add(options: EventsPublisherOptions) {
  }
}

/**
 * An event store to append new events
 */
interface EventStore<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void>
}

/**
 * A repository for snapshots
 */
interface SnapshotRepository<A : AggregateRoot, C : Command, E : DomainEvent> {
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
