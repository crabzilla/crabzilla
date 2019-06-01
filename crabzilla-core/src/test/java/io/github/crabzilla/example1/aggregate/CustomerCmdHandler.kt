package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler

class CustomerCmdHandler(cmdMetadata: CommandMetadata,
                         command: Command,
                         snapshot: Snapshot<Customer>,
                         stateFn: (DomainEvent, Customer) -> Customer,
                         uowHandler: Handler<AsyncResult<UnitOfWork>>) :
  CommandHandler<Customer>("customer", cmdMetadata, command, snapshot, stateFn, uowHandler) {

  override fun handleCommand() {

    val customer = snapshot.state

    when (command) {
      is CreateCustomer -> customer.create(CustomerId(cmdMetadata.entityId), command.name, eventsFuture.completer())
      is ActivateCustomer -> eventsFuture.complete(customer.activate(command.reason))
      is DeactivateCustomer -> customer.deactivate(command.reason, eventsFuture)
      is CreateActivateCustomer -> {
        val createFuture: Future<List<DomainEvent>> = Future.future()
        val tracker = StateTransitionsTracker(snapshot, stateFn)
        tracker.currentState.create(CustomerId(cmdMetadata.entityId), command.name, createFuture)
        createFuture
          .compose { v ->
            println("after create")
            println("  events $v")
            tracker.applyEvents(v)
            println("  state " + tracker.currentState)
            Future.succeededFuture(tracker.currentState.activate("I can"))
          }
          .compose { v ->
            println("after activate")
            println("  events $v")
            tracker.applyEvents(v)
            println("  state " + tracker.currentState)
            Future.succeededFuture(tracker.appliedEvents)
          }
          .compose({ v ->
            println("after collect all events")
            println("  events $v")
          }, eventsFuture)
      }
      else -> eventsFuture.fail("${cmdMetadata.commandName} is a unknown command")
    }
  }
}
