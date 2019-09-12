package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.framework.*

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
       else -> listOf() // all other commands are valid
     }
   }

  override val cmdHandlerFactory: (cmdMetadata: CommandMetadata,
                                   command: Command,
                                   snapshot: Snapshot<Customer>) -> EntityCommandHandler<Customer> = {
    cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<Customer> ->
            CustomerCmdHandler(cmdMetadata, command, snapshot, applyEvent)
  }

}

