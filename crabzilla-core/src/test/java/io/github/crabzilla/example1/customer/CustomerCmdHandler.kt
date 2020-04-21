package io.github.crabzilla.example1.customer

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.CommandMetadata
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.EntityCommandHandler
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.framework.StateTransitionsTracker
import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise

class CustomerCmdHandler(
  cmdMetadata: CommandMetadata,
  command: Command,
  snapshot: Snapshot<Customer>,
  stateFn: (DomainEvent, Customer) -> Customer
) :
  EntityCommandHandler<Customer>("customer", cmdMetadata, command, snapshot, stateFn) {

  override fun handleCommand(): Future<UnitOfWork> {
    val customer = snapshot.state
    val eventsPromise: Future<List<DomainEvent>> =
      when (command) {
        is CreateCustomer -> customer.create(cmdMetadata.entityId, command.name)
        is ActivateCustomer -> succeededFuture(customer.activate(command.reason))
        is DeactivateCustomer -> customer.deactivate(command.reason)
        is CreateActivateCustomer -> composed(command)
        else -> Future.failedFuture("${cmdMetadata.commandName} is a unknown command")
    }
    return fromEvents(eventsPromise)
  }

  private fun composed(command: CreateActivateCustomer): Future<List<DomainEvent>> {
    val promise = Promise.promise<List<DomainEvent>>()
    val tracker = StateTransitionsTracker(snapshot, stateFn)
    tracker.currentState
      .create(cmdMetadata.entityId, command.name)
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        succeededFuture(tracker.currentState.activate(command.reason))
      }
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        promise.complete(tracker.appliedEvents)
        promise.future()
      }
    return promise.future()
  }
}
