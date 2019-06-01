package io.github.crabzilla.example1

import io.github.crabzilla.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import java.time.Instant

data class CustomerId(val value: Int)

// events

data class CustomerCreated(val customerId: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// commands

data class CreateCustomer(val name: String) : Command

data class ActivateCustomer(val reason: String) : Command

data class DeactivateCustomer(val reason: String) : Command

data class CreateActivateCustomer(val name: String, val reason: String) : Command

// just for test

data class UnknownCommand(val id: CustomerId) : Command

// command handler

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

// values an functions

val CUSTOMER_SEED_VALUE = Customer()

val CUSTOMER_STATE_BUILDER = { event: DomainEvent, customer: Customer ->
  when (event) {
    is CustomerCreated -> customer.copy(customerId = event.customerId, name = event.name, isActive = false)
    is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
    else -> customer
  }
}

// https://github.com/konform-kt/konform
val CUSTOMER_CMD_VALIDATOR = { command: Command ->
  when (command) {
    is CreateCustomer ->
      if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
    else -> listOf() // all other commands are valid
  }
}

// boilerplate

// TODO  default impl for this
val CUSTOMER_CMD_HANDLER_FACTORY: CommandHandlerFactory<Customer> = { cmdMetadata: CommandMetadata,
                                                                      command: Command,
                                                                      snapshot: Snapshot<Customer>,
                                                                      uowHandler: Handler<AsyncResult<UnitOfWork>> ->
  CustomerCmdHandler(cmdMetadata, command, snapshot, CUSTOMER_STATE_BUILDER, uowHandler)
}

enum class CustomerCommandEnum {
  CREATE, ACTIVATE, DEACTIVATE, CREATE_ACTIVATE;
  fun urlFriendly() : String {
    return this.name.toLowerCase().replace('_', '-')
  }
}

class CustomerFn : EntityCommandAware<Customer> {

  override fun initialState(): Customer {
    return Customer()
  }

  override fun applyEvent(event: DomainEvent, state: Customer): Customer {
    return CUSTOMER_STATE_BUILDER.invoke(event, state)
  }

  override fun validateCmd(command: Command): List<String> {
    return CUSTOMER_CMD_VALIDATOR.invoke(command)
  }

  override fun cmdHandlerFactory(): CommandHandlerFactory<Customer> {
    return CUSTOMER_CMD_HANDLER_FACTORY
  }

}

