package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StateTransitionsTracker
import io.vertx.core.Future
import io.vertx.core.Promise

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

  override val validateCmd: (command: Command) -> List<String> = {
    command ->
    when (command) {
      is CreateCustomer ->
        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
      is CreateActivateCustomer ->
        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
      is ActivateCustomer, is DeactivateCustomer -> listOf()
      else -> listOf("invalid command ${command.javaClass.simpleName}") // all other commands are invalid
    }
  }

  override val handleCmd: (Triple<CommandMetadata, Command, Snapshot<Customer>>) -> Future<List<DomainEvent>> = {
    request ->
    val (cmdMetadata, command, snapshot) = request
    val customer = snapshot.state
    when (command) {
      is CreateCustomer -> customer.create(cmdMetadata.entityId, command.name)
      is ActivateCustomer -> Future.succeededFuture(customer.activate(command.reason))
      is DeactivateCustomer -> customer.deactivate(command.reason)
      is CreateActivateCustomer -> createActivate(request)
      else -> Future.failedFuture("${cmdMetadata.commandName} is a unknown command")
    }
  }

  private fun createActivate(cmdRequest: Triple<CommandMetadata, Command, Snapshot<Customer>>):
    Future<List<DomainEvent>> {
    val promise = Promise.promise<List<DomainEvent>>()
    val (cmdMetadata, command, snapshot) = cmdRequest
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
