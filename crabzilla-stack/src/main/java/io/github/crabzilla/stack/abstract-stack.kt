package io.github.crabzilla.stack

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future
import java.util.UUID

// es/cqrs infra stack

@JvmInline
value class AggregateRootId(val id: UUID)

@JvmInline
value class CommandId(val id: UUID = UUID.randomUUID())

@JvmInline
value class CorrelationId(val id: UUID)

@JvmInline
value class CausationId(val id: UUID)

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
  fun upsert(id: UUID, snapshot: Snapshot<A>): Future<Void>
}

/**
 * To scan for new events
 */
interface EventsScanner {
  fun streamName(): String
  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>>
  fun updateOffSet(eventId: Long): Future<Void>
}

/**
 * To publish an event as JSON to read model, messaging broker, etc (any side effect)
 */
interface EventsPublisher {
  fun publish(eventRecord: EventRecord): Future<Void>
  // TODO what about correlation id, etc?
}
