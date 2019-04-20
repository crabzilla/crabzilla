package io.github.crabzilla.example1

import io.github.crabzilla.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.time.Instant
import java.util.*

data class CustomerId(val value: Int)

// events

data class CustomerCreated(val customerId: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// commands

data class CreateCustomer(val customerId: CustomerId, val name: String) : Command

data class ActivateCustomer(val customerId: CustomerId, val reason: String) : Command

data class DeactivateCustomer(val customerId: CustomerId, val reason: String) : Command

data class CreateActivateCustomer(val customerId: CustomerId, val name: String, val reason: String) : Command

// just for test

data class UnknownCommand(val id: CustomerId) : Command

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

val CUSTOMER_SEED_VALUE = Customer()

val CUSTOMER_FROM_JSON = { json: JsonObject ->
  Customer(CustomerId(json.getInteger("customerId")), json.getString("name"), json.getBoolean("isActive"),
    json.getString("reason"))
}

val CUSTOMER_STATE_BUILDER = { event: DomainEvent, customer: Customer ->
  when (event) {
    is CustomerCreated -> customer.copy(customerId = event.customerId, name = event.name, isActive = false)
    is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
    else -> customer
  }
}

val CUSTOMER_EVENT_FROM_JSON = { eventName: String, jo: JsonObject ->
  when (eventName) {
    "CustomerCreated" -> CustomerCreated(CustomerId(jo.getJsonObject("customerId").getInteger("value")),
      jo.getString("name"))
    "CustomerActivated" -> CustomerActivated(jo.getString("reason"), jo.getInstant("_when"))
    "CustomerDeactivated" -> CustomerDeactivated(jo.getString("reason"), jo.getInstant("_when"))
    else -> throw java.lang.IllegalArgumentException("$eventName is unknown")
  }
}

val CUSTOMER_CMD_FROM_JSON = { cmdName: String, jo: JsonObject ->
  when (cmdName) {
    "create" -> CreateCustomer(CustomerId(jo.getJsonObject("customerId").getInteger("value")), jo.getString("name"))
    "activate" -> ActivateCustomer(CustomerId(jo.getInteger("customerId")), jo.getString("reason"))
    "deactivate" -> DeactivateCustomer(CustomerId(jo.getInteger("customerId")), jo.getString("reason"))
    "create-activate" -> CreateActivateCustomer(CustomerId(jo.getInteger("customerId")), jo.getString("name"),
      jo.getString("reason"))
    else -> throw IllegalArgumentException("$cmdName is unknown")
  }
}

val CUSTOMER_EVENT_TO_JSON = { event: DomainEvent ->
  when (event) {
    is CustomerCreated -> Json.encode(event)
    is CustomerActivated -> Json.encode(event)
    is CustomerDeactivated -> Json.encode(event)
    else -> throw java.lang.IllegalArgumentException("$event is unknown")
  }
}

val CUSTOMER_CMD_TO_JSON = { cmd: Command ->
  when (cmd) {
    is CreateCustomer -> Json.encode(cmd)
    is ActivateCustomer -> Json.encode(cmd)
    is DeactivateCustomer -> Json.encode(cmd)
    is CreateActivateCustomer -> Json.encode(cmd)
    else -> throw IllegalArgumentException("$cmd is unknown")
  }
}

// https://github.com/konform-kt/konform
val CUSTOMER_CMD_VALIDATOR = { command: Command ->
  when (command) {
    is CreateCustomer ->
      if (command.name.equals("a bad name")) listOf("Invalid name: ${command.name}") else listOf()
    else -> listOf() // all other commands are valid
  }
}

val CUSTOMER_CMD_HANDLER_FACTORY: CommandHandlerFactory<Customer> = { targetId: Int,
                                                                      targetName: String,
                                                                      commandId: UUID,
                                                                      commandName: String,
                                                                      command: Command,
                                                                      snapshot: Snapshot<Customer>,
                                                                      uowHandler: Handler<AsyncResult<UnitOfWork>> ->
  CustomerCmdHandler(targetId, targetName, commandId, commandName, command, snapshot, CUSTOMER_STATE_BUILDER, uowHandler)
}

class CustomerCmdHandler(targetId: Int,
                         targetName: String,
                         commandId: UUID,
                         commandName: String,
                         command: Command,
                         snapshot: Snapshot<Customer>,
                         stateFn: (DomainEvent, Customer) -> Customer,
                         uowHandler: Handler<AsyncResult<UnitOfWork>>) :
  CommandHandler<Customer>(targetId, targetName, commandId, commandName, command, snapshot, stateFn, uowHandler) {

  override fun handleCommand() {

    val customer = snapshot.instance

    when (command) {
      is CreateCustomer -> customer.create(command.customerId, command.name, eventsFuture.completer())
      is ActivateCustomer -> eventsFuture.complete(customer.activate(command.reason))
      is DeactivateCustomer -> customer.deactivate(command.reason, eventsFuture.completer())
      is CreateActivateCustomer -> {
        val createFuture: Future<List<DomainEvent>> = Future.future()
        val tracker = StateTransitionsTracker(snapshot, stateFn)
        tracker.currentState.create(command.customerId, command.name, createFuture)
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
      else -> uowFuture.fail("${commandName} is a unknown command")
    }
  }
}

enum class CustomerCommandEnum {
  CREATE, ACTIVATE, DEACTIVATE, CREATE_ACTIVATE;
  fun asPathParam() : String {
    return this.name.toLowerCase().replace('_', '-')
  }
}

// TODO considerar
//  memoized methods (devolvendo functions em vez de valores)
//  Try monad (wrap function execution)
