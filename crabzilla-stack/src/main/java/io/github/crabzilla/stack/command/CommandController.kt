package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.CommandException.ValidationException
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandHandlerApi
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future
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
) {
  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>> {
    log.debug("received {}", command)
    val validator: CommandValidator<C>? = config.commandValidator
    if (validator != null) {
      val validationErrors = validator.validate(command)
      if (validationErrors.isNotEmpty()) {
        return Future.failedFuture(ValidationException(validationErrors))
      }
    }
    log.debug("Will get snapshot for aggregate {}", metadata.domainStateId)
    return snapshotRepo.get(metadata.domainStateId.id)
      .compose { snapshot ->
        log.debug("Got snapshot {}. Now let's handle the command", snapshot)
        when (val handler: CommandHandlerApi<A, C, E> = config.commandHandlerFactory.invoke()) {
          is CommandHandler<A, C, E> -> {
            try {
              Future.succeededFuture(handler.handleCommand(command, config.eventHandler, snapshot))
            } catch (e: Throwable) {
              Future.failedFuture(e)
            }
          }
          is FutureCommandHandler<A, C, E> -> {
            handler.handleCommand(command, config.eventHandler, snapshot)
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
