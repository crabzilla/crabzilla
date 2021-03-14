package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.serialization.json.Json
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
    if (log.isDebugEnabled) log.debug("received $metadata\n $command")
    val validationErrors = validator.validate(command)
    if (validationErrors.isNotEmpty()) {
      promise.complete(Either.Left(validationErrors))
      return promise.future()
    }
    snapshotRepo.get(metadata.aggregateRootId)
      .onSuccess { snapshot ->
        handler.handleCommand(command, snapshot)
          .onFailure { err -> promise.fail(err.cause) }
          .onSuccess { result: StatefulSession<A, E> ->
            eventStore.append(command, metadata, result)
              .onFailure { err -> promise.fail(err.cause) }
              .onSuccess {
                val newSnapshot = Snapshot(result.currentState, result.originalVersion + 1)
                snapshotRepo.upsert(metadata.aggregateRootId, newSnapshot)
                  .onFailure { err ->
                    log.error("When saving new snapshot", err)
                  }
                  .onComplete {
                    // let's just ignore snapshot error (the principal side effect is on eventSTore, anyway)
                    promise.complete(Either.Right(result))
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

inline class AggregateRootName(val value: String)
inline class SnapshotTableName(val value: String)

class AggregateRootConfig<A : AggregateRoot, C : Command, E : DomainEvent> (
  val name: AggregateRootName,
  val snapshotTableName: SnapshotTableName,
  val eventHandler: EventHandler<A, E>,
  val commandValidator: CommandValidator<C>,
  val commandHandler: CommandHandler<A, C, E>,
  val json: Json
)

sealed class Either<out A, out B> {
  class Left<A>(val value: A) : Either<A, Nothing>()
  class Right<B>(val value: B) : Either<Nothing, B>()
}
