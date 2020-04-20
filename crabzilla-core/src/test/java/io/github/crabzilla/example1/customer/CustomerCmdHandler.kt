package io.github.crabzilla.example1.customer

import io.github.crabzilla.framework.*
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise

class CustomerCmdHandler(cmdMetadata: CommandMetadata,
                         command: Command,
                         snapshot: Snapshot<Customer>,
                         stateFn: (DomainEvent, Customer) -> Customer) :
  EntityCommandHandler<Customer>("customer", cmdMetadata, command, snapshot, stateFn) {

  override fun handleCommand(): Promise<UnitOfWork> {
    val customer = snapshot.state
    val eventsPromise: Promise<List<DomainEvent>> =
      when (command) {
        is CreateCustomer -> customer.create(cmdMetadata.entityId, command.name)
        is ActivateCustomer -> succeededPromise(customer.activate(command.reason))
        is DeactivateCustomer -> customer.deactivate(command.reason)
        is CreateActivateCustomer -> composed(command)
        else -> failedPromise("${cmdMetadata.commandName} is a unknown command")
    }
    return fromEvents(eventsPromise)
  }

  private fun composed(command: CreateActivateCustomer) : Promise<List<DomainEvent>> {
    val promise = Promise.promise<List<DomainEvent>>()
    val tracker = StateTransitionsTracker(snapshot, stateFn)
    tracker.currentState
      .create(cmdMetadata.entityId, command.name)
      .future()
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        succeededFuture(tracker.currentState.activate(command.reason))
      }
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        promise.complete(tracker.appliedEvents)
        promise.future()
      }
    return promise
  }

}
