package io.github.crabzilla.stack

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
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
  private val config: AggregateRootConfig<A, C, E>,
  private val snapshotRepo: SnapshotRepository<A>, // could be public?
  private val eventStore: EventStore<A, C, E>
) {
  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>> {
    val promise = Promise.promise<StatefulSession<A, E>>()
    log.debug("received {}", command)
    val validationErrors = config.commandValidator.validate(command)
    if (validationErrors.isNotEmpty()) {
      promise.fail(CommandException.ValidationException(validationErrors))
      return promise.future()
    }
    log.debug("Will get snapshot for aggregate {}", metadata.aggregateRootId)
    snapshotRepo.get(metadata.aggregateRootId.id)
      .onFailure { err ->
        promise.fail(err)
      }
      .onSuccess { snapshot ->
        log.debug("Got snapshot {}. Now let's handle the command", snapshot)
        try {
          val session = config.commandHandler.handleCommand(command, snapshot, config.eventHandler)
          log.debug("Command handled. {}. Now let's append it events", session.toSessionData())
          eventStore.append(command, metadata, session)
            .onFailure { err ->
              promise.fail(err)
            }
            .onSuccess {
              log.debug("Events successfully appended.")
              promise.complete(session)
            }
        } catch (e: Throwable) {
          promise.fail(e)
        }
      }
    return promise.future()
  }
}
