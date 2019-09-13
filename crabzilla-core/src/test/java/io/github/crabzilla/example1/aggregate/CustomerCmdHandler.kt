package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.example1.*
import io.github.crabzilla.framework.*
import io.vertx.core.Future
import io.vertx.core.Future.*
import io.vertx.core.Promise
import io.vertx.core.Promise.failedPromise
import io.vertx.core.Promise.succeededPromise

class CustomerCmdHandler(cmdMetadata: CommandMetadata,
                         command: Command,
                         snapshot: Snapshot<Customer>,
                         stateFn: (DomainEvent, Customer) -> Customer) :
  EntityCommandHandler<Customer>("customer", cmdMetadata, command, snapshot, stateFn) {

  override fun handleCommand(): Promise<UnitOfWork> {
    val customer = snapshot.state
    val eventsPromise: Promise<List<DomainEvent>> =
      when (command) {
        is CreateCustomer -> customer.create(CustomerId(cmdMetadata.entityId), command.name)
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
    tracker.currentState.create(CustomerId(cmdMetadata.entityId), command.name).future()
      .compose { v ->
        tracker.applyEvents(v)
        succeededFuture(tracker.currentState.activate("I can"))
      }
      .compose { v ->
        tracker.applyEvents(v)
        succeededFuture(tracker.appliedEvents)
      }
      .compose({ eventsList ->
        promise.complete(eventsList)
      }, promise.future())
    return promise
  }

}
