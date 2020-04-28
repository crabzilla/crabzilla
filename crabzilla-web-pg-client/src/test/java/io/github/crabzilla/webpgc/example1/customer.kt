package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandContext
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.StateTransitionsTracker
import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

typealias CustomerId = Int

// events

@Serializable
data class CustomerCreated(val customerId: CustomerId, val name: String) : DomainEvent()

@Serializable
data class CustomerActivated(val reason: String) : DomainEvent()

@Serializable
data class CustomerDeactivated(val reason: String) : DomainEvent()

// commands

@Serializable
data class CreateCustomer(val name: String) : Command()

@Serializable
data class ActivateCustomer(val reason: String) : Command()

@Serializable
data class DeactivateCustomer(val reason: String) : Command()

@Serializable
data class CreateActivateCustomer(val name: String, val reason: String) : Command()

@Serializable
data class UnknownCommand(val x: Int) : Command()

// aggregate root

@Serializable
data class Customer(
  val customerId: CustomerId? = null,
  val name: String? = null,
  val isActive: Boolean? = false,
  val reason: String? = null
) : Entity() {

  fun create(id: CustomerId, name: String): Future<List<DomainEvent>> {
    require(this.customerId == null) { "customer already created" }
    return Future.succeededFuture(listOf(CustomerCreated(id, name)))
  }

  fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return listOf(CustomerActivated(reason))
  }

  fun deactivate(reason: String): Future<List<DomainEvent>> {
    customerMustExist()
    return Future.succeededFuture(listOf(CustomerDeactivated(reason)))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }
}

// command aware

class CustomerCommandAware : EntityCommandAware<Customer> {

  override val initialState: Customer = Customer()

  override val applyEvent: (event: DomainEvent, state: Customer) -> Customer = {
    event: DomainEvent, customer: Customer ->
    when (event) {
      is CustomerCreated -> customer.copy(customerId = event.customerId, name = event.name, isActive = false)
      is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
      is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
      else -> customer
    }
  }

  override val validateCmd: (command: Command) -> List<String> = { command ->
    when (command) {
      is CreateCustomer ->
        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
      is CreateActivateCustomer ->
        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
      is ActivateCustomer, is DeactivateCustomer -> listOf()
      else -> listOf("invalid command ${command.javaClass.simpleName}") // all other commands are invalid
    }
  }

  override val handleCmd: (CommandContext<Customer>) -> Future<List<DomainEvent>> = { ctx ->
    val (cmdMetadata, command, snapshot) = ctx
    val customer = snapshot.state
    when (command) {
      is CreateCustomer -> customer.create(cmdMetadata.entityId, command.name)
      is ActivateCustomer -> Future.succeededFuture(customer.activate(command.reason))
      is DeactivateCustomer -> customer.deactivate(command.reason)
      is CreateActivateCustomer -> createActivate(ctx)
      else -> Future.failedFuture("${cmdMetadata.commandName} is a unknown command")
    }
  }

  private fun createActivate(ctx: CommandContext<Customer>): Future<List<DomainEvent>> {
    val promise = Promise.promise<List<DomainEvent>>()
    val (cmdMetadata, command, snapshot) = ctx
    val tracker = StateTransitionsTracker(snapshot, applyEvent)
    val cmd = command as CreateActivateCustomer
    tracker.currentState
      .create(cmdMetadata.entityId, cmd.name)
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        Future.succeededFuture(tracker.currentState.activate(cmd.reason))
      }
      .compose { eventsList ->
        tracker.applyEvents(eventsList)
        promise.complete(tracker.appliedEvents)
        promise.future()
      }
    return promise.future()
  }
}

// kotlinx.serialization

val customerModule = SerializersModule {
  polymorphic(Entity::class) {
    Customer::class with Customer.serializer()
  }
  polymorphic(Command::class) {
    CreateCustomer::class with CreateCustomer.serializer()
    ActivateCustomer::class with ActivateCustomer.serializer()
    DeactivateCustomer::class with DeactivateCustomer.serializer()
    CreateActivateCustomer::class with CreateActivateCustomer.serializer()
    UnknownCommand::class with UnknownCommand.serializer()
  }
  polymorphic(DomainEvent::class) {
    CustomerCreated::class with CustomerCreated.serializer()
    CustomerActivated::class with CustomerActivated.serializer()
    CustomerDeactivated::class with CustomerDeactivated.serializer()
  }
}
