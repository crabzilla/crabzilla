package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.EntityCommandHandler
import io.github.crabzilla.core.Snapshot

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

  override val cmdHandlerFactory: (
    cmdMetadata: CommandMetadata,
    command: Command,
    snapshot: Snapshot<Customer>
  ) -> EntityCommandHandler<Customer> = {
    cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<Customer> ->
            CustomerCmdHandler(cmdMetadata, command, snapshot, applyEvent)
  }
}
