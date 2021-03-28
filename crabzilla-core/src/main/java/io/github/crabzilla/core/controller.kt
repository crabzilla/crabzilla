package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Given a command
 *  1 - validates it
 *  2 - find the target snapshot
 *  3 - call it's command handler
 *  4 - save the resulting events in eventstore
 *  5 - save the new snapshot
 */
class CommandController<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val validator: CommandValidator<C>,
  private val handler: CommandHandler<A, C, E>,
  private val snapshotRepo: SnapshotRepository<A, C, E>,
  private val eventStore: EventStore<A, C, E>
) {
  companion object {
    // TODO https://reactiverse.io/reactiverse-contextual-logging/
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: C): Future<Either<List<String>, StatefulSession<A, E>>> {
    // TODO also return metadata (internal ids)
    val promise = Promise.promise<Either<List<String>, StatefulSession<A, E>>>()
    log.info("received $command")
    val validationErrors = validator.validate(command)
    if (validationErrors.isNotEmpty()) {
      promise.complete(Either.Left(validationErrors))
      log.error("Invalid command $metadata\n $command \n$validationErrors")
      return promise.future()
    }
    log.info("Will get snapshot for aggregate ${metadata.aggregateRootId}")
    snapshotRepo.get(metadata.aggregateRootId)
      .onFailure { err ->
        log.error("Could not get snapshot", err)
        promise.fail(err.cause)
      }
      .onSuccess { snapshot ->
        log.info("Got snapshot $snapshot. Now let's handle the command")
        handler.handleCommand(command, snapshot)
          .onFailure { err ->
            log.error("Command error", err)
            promise.fail(err.cause)
          }
          .onSuccess { result: StatefulSession<A, E> ->
            log.info("Command handled. ${result.currentState}")
            log.info("Now let's append events ${result.appliedEvents()}")
            eventStore.append(command, metadata, result)
              .onFailure { err ->
                log.error("When appending events", err)
                promise.fail(err.cause)
              }
              .onSuccess {
                log.info("Events successfully appended. Now let's save the resulting snapshot")
                val newSnapshot = Snapshot(result.currentState, result.originalVersion + 1)
                promise.complete(Either.Right(result))
                snapshotRepo.upsert(metadata.aggregateRootId, newSnapshot)
                  .onFailure { err ->
                    log.error("When saving new snapshot", err)
                    // let's just ignore snapshot error (the principal side effect is on eventSTore, anyway)
                  }
                  .onSuccess {
                    log.info("Snapshot upsert done: $newSnapshot")
                  }
              }
          }
      }
    return promise.future()
  }
}

/**
 * The client must knows how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: Int,
  val id: UUID = UUID.randomUUID(),
  val causationId: UUID = id,
  val correlationID: UUID = id
)

sealed class Either<out A, out B> {
  class Left<A>(val value: A) : Either<A, Nothing>()
  class Right<B>(val value: B) : Either<Nothing, B>()
}

/**
 * To perform aggregate root business methods and track it's events and state
 */
class StatefulSession<A : AggregateRoot, E : DomainEvent> {
  val originalVersion: Int
  private val originalState: A
  private val eventHandler: EventHandler<A, E>
  private val appliedEvents = mutableListOf<E>()
  var currentState: A

  constructor(version: Int, state: A, eventHandler: EventHandler<A, E>) {
    this.originalVersion = version
    this.originalState = state
    this.eventHandler = eventHandler
    this.currentState = originalState
  }

  constructor(constructorResult: CommandHandler.ConstructorResult<A, E>, eventHandler: EventHandler<A, E>) {
    this.originalVersion = 0
    this.originalState = constructorResult.state
    this.eventHandler = eventHandler
    this.currentState = originalState
    constructorResult.events.forEach {
      appliedEvents.add(it)
    }
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }

  fun apply(events: List<E>): StatefulSession<A, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.handleEvent(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun execute(fn: (A) -> List<E>): StatefulSession<A, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }

  fun toSessionData(): SessionData {
    return SessionData(originalVersion, if (originalVersion == 0) null else originalState, appliedEvents, currentState)
  }
}

data class SessionData(
  val originalVersion: Int,
  val originalState: AggregateRoot?,
  val events: List<DomainEvent>,
  val newState: AggregateRoot
)
