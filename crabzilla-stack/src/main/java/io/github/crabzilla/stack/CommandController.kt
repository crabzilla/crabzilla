package io.github.crabzilla.stack

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.LoggerFactory

/**
 * Given a command
 *  1 - validates it
 *  2 - find the target snapshot
 *  3 - call it's command handler
 *  4 - save the resulting events in event store
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

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>> {
    // TODO also return metadata (internal ids)
    val promise = Promise.promise<StatefulSession<A, E>>()
    if (log.isDebugEnabled) log.debug("received $command")
    val validationErrors = validator.validate(command)

    if (validationErrors.isNotEmpty()) {
      promise.fail(ValidationException(validationErrors))
      log.error("Invalid command $metadata\n $command \n$validationErrors")
      return promise.future()
    }
    if (log.isDebugEnabled) log.debug("Will get snapshot for aggregate ${metadata.aggregateRootId}")
    snapshotRepo.get(metadata.aggregateRootId.id)
      .onFailure { err ->
        log.error("Could not get snapshot", err)
        promise.fail(err)
      }
      .onSuccess { snapshot ->
        if (log.isDebugEnabled) log.debug("Got snapshot $snapshot. Now let's handle the command")
        handler.handleCommand(command, snapshot)
          .onFailure { err ->
            log.error("Command error", err)
            promise.fail(err)
          }
          .onSuccess { result: StatefulSession<A, E> ->
            if (log.isDebugEnabled) log.debug("Command handled. ${result.toSessionData()}. Now let's append it events")
            eventStore.append(command, metadata, result)
              .onFailure { err ->
                log.error("When appending events", err)
                promise.fail(err)
              }
              .onSuccess {
                if (log.isDebugEnabled) log.debug("Events successfully appended.")
                promise.complete(result)
              }
          }
      }
    return promise.future()
  }
}
