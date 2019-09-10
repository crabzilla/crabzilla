package io.github.crabzilla.example1.aggregate

import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.example1.aggregate.CustomerCommandAware.Companion.CUSTOMER_STATE_BUILDER
import io.github.crabzilla.framework.*
import io.github.crabzilla.framework.CommandMetadata
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class CustomerCommandAware : EntityCommandAware<Customer> {

  companion object {
    val state = Customer()
    val cmdHandlerFactory = CustomerCmdHandlerFactory()
    val CUSTOMER_STATE_BUILDER = { event: DomainEvent, customer: Customer ->
      when (event) {
        is CustomerCreated -> customer.copy(customerId = event.customerId, name = event.name, isActive = false)
        is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
        is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
        else -> customer
      }
    }
  }

  override fun initialState(): Customer {
    return state
  }

  override fun applyEvent(event: DomainEvent, state: Customer): Customer {
    return CUSTOMER_STATE_BUILDER.invoke(event, state)
  }

  override fun validateCmd(command: Command): List<String> {
    return when (command) {
      is CreateCustomer ->
        if (command.name == "a bad name") listOf("Invalid name: ${command.name}") else listOf()
      else -> listOf() // all other commands are valid
    }
  }

  override fun cmdHandlerFactory(): EntityCommandHandlerFactory<Customer> {
    return cmdHandlerFactory
  }

}

class CustomerCmdHandlerFactory : EntityCommandHandlerFactory<Customer> {
  override fun createHandler(cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<Customer>,
                             handler: Handler<AsyncResult<UnitOfWork>>): EntityCommandHandler<Customer> {
    return CustomerCmdHandler(cmdMetadata, command, snapshot, CUSTOMER_STATE_BUILDER, handler)
  }
}
