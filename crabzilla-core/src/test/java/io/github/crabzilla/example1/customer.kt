package io.github.crabzilla.example1

import io.github.crabzilla.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import java.time.Instant
import java.util.*

// TODO replace with inline classes
data class CustomerId(val id: Int) : EntityId {
  override fun value(): Int {
    return id
  }
}

// events

data class CustomerCreated(val id: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// commands

data class CreateCustomer(override val commandId: UUID, override val targetId: CustomerId, val name: String) : Command

data class ActivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val reason: String)
  : Command

data class DeactivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val reason: String)
  : Command

data class CreateActivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val name: String,
                                  val reason: String) : Command

// just for test

data class
UnknownCommand(override val commandId: UUID, override val targetId: CustomerId) : Command

// aggregate root

data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null) : Entity {

  // TODO perform a query on read model to validate name uniqueness
  fun create(id: CustomerId, name: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
    require(this.customerId == null) { "customer already created" }
    handler.handle(Future.succeededFuture(eventsOf(CustomerCreated(id, name))))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, Instant.now()))
  }

  // TODO perform a web request ask if this operation is allowed
  fun deactivate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
    handler.handle(Future.future { event ->
      customerMustExist()
      event.complete(eventsOf(CustomerDeactivated(reason, Instant.now())))
    })
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }

}

val CUSTOMER_SEED_VALUE = Customer(null, null, null, null)

val CUSTOMER_STATE_BUILDER = { event: DomainEvent, customer: Customer ->
  when (event) {
    is CustomerCreated -> customer.copy(customerId = event.id, name = event.name, isActive = false)
    is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
    else -> customer
  }
}

val CUSTOMER_CMD_VALIDATOR = { command: Command ->
  when (command) {
    is CreateCustomer ->
      if (command.name.equals("a bad name")) listOf("Invalid name: ${command.name}") else listOf()
    else -> listOf() // all other commands are valid
  }
}

val CUSTOMER_CMD_HANDLER_FACTORY = { command: Command,
                                     snapshot: Snapshot<Customer>,
                                     uowHandler: Handler<AsyncResult<UnitOfWork>> ->
  CustomerCmdHandler(command, snapshot, CUSTOMER_STATE_BUILDER, uowHandler)
}

class CustomerCmdHandler(command: Command, snapshot: Snapshot<Customer>,
                         stateFn: (DomainEvent, Customer) -> Customer,
                         uowHandler: Handler<AsyncResult<UnitOfWork>>) :
  CommandHandler<Customer>(command, snapshot, stateFn, uowHandler) {

  //  val events = tracker
//    .applyEvents { c -> c.create(cmd.targetId, cmd.name) }
//    .applyEvents { c -> c.activate(cmd.reason) }
//    .collectEvents()
//
  override fun handleCommand() {

    val uowFuture: Future<UnitOfWork> = Future.future()
    uowFuture.setHandler(uowHandler)

    val eventsFuture: Future<List<DomainEvent>> = Future.future()

    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(UnitOfWork.of(command, event.result(), snapshot.version + 1))
      } else {
        uowFuture.fail(event.cause())
      }
    }

    val customer = snapshot.instance

    when (command) {
      is CreateCustomer -> customer.create(command.targetId, command.name, eventsFuture.completer())
      is ActivateCustomer -> eventsFuture.complete(customer.activate(command.reason))
      is DeactivateCustomer -> customer.deactivate(command.reason, eventsFuture.completer())
      is CreateActivateCustomer -> {
        val createFuture: Future<List<DomainEvent>> = Future.future()
        val tracker = StateTransitionsTracker(snapshot, stateFn)
        tracker.currentState.create(command.targetId, command.name, createFuture)
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
      else -> uowFuture.fail("${command.javaClass.simpleName} is a unknown command")
    }
  }
}
