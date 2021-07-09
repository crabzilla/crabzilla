package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandHandlerApi
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.stack.command.CommandException.ValidationException
import io.github.crabzilla.stack.storage.EventStore
import io.github.crabzilla.stack.storage.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory

/**
 * Given a command
 *  1 - validates it
 *  2 - find the target snapshot
 *  3 - call it's command handler
 *  4 - save the resulting events in event store
 */
class CommandController<A : DomainState, C : Command, E : DomainEvent>(
  private val config: CommandControllerConfig<A, C, E>,
  private val snapshotRepo: SnapshotRepository<A>, // could be public?
  private val eventStore: EventStore<A, C, E>,
  private val eventBus: EventBus,
) {
  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>> {
    log.debug("received {}", command)
    if (config.commandValidator != null) {
      val validationErrors = config.commandValidator.validate(command)
      if (validationErrors.isNotEmpty()) {
        return Future.failedFuture(ValidationException(validationErrors))
      }
    }
    log.debug("Will get snapshot for aggregate {}", metadata.domainStateId)
    return snapshotRepo.get(metadata.domainStateId.id)
      .compose { snapshot ->
        log.debug("Got snapshot {}. Now let's handle the command", snapshot)
        when (val handler: CommandHandlerApi<A, C, E> = config.commandHandler) {
          is CommandHandler<A, C, E> -> {
            try {
              Future.succeededFuture(handler.handleCommand(command, config.eventHandler, snapshot))
            } catch (e: Throwable) {
              Future.failedFuture(e)
            }
          }
          is ExternalCommandHandler<A, C, E> -> {
            handler.handleCommand(eventBus, command, config.eventHandler, snapshot)
          }
          else -> Future.failedFuture("Unknown command handler")
        }
          .compose { session: StatefulSession<A, E> ->
            log.debug("Command handled. {}. Now let's append it events", session.toSessionData())
            eventStore.append(command, metadata, session)
              .map { session }
          }
      }
  }
}
