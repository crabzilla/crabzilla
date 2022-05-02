package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.core.FeatureSession
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import java.util.UUID

sealed class CustomerEvent {
  data class CustomerRegistered(val id: UUID, val name: String) : CustomerEvent()
  data class CustomerActivated(val reason: String) : CustomerEvent()
  data class CustomerDeactivated(val reason: String) : CustomerEvent()
}

sealed class CustomerCommand {
  data class RegisterCustomer(val customerId: UUID, val name: String) : CustomerCommand()
  data class ActivateCustomer(val reason: String) : CustomerCommand()
  data class DeactivateCustomer(val reason: String) : CustomerCommand()
  data class RegisterAndActivateCustomer(
    val customerId: UUID,
    val name: String,
    val reason: String
  ) : CustomerCommand()
}

data class Customer(val id: UUID, val name: String, val isActive: Boolean = false, val reason: String? = null) {
  companion object {
    fun create(id: UUID, name: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }
    fun fromEvent(event: CustomerRegistered): Customer {
      return Customer(id = event.id, name = event.name, isActive = false)
    }
  }
  fun activate(reason: String): List<CustomerEvent> {
    return listOf(CustomerActivated(reason))
  }
  fun deactivate(reason: String): List<CustomerEvent> {
    return listOf(CustomerDeactivated(reason))
  }
}

val customerCmdValidator = CommandValidator<CustomerCommand> { command ->
  when (command) {
    is RegisterCustomer -> if (command.name == "bad customer") listOf("Bad customer!") else listOf()
    is RegisterAndActivateCustomer -> listOf()
    is ActivateCustomer -> listOf()
    is DeactivateCustomer -> listOf()
  }
}

val customerEventHandler = EventHandler<Customer, CustomerEvent> { state, event ->
  when (event) {
    is CustomerRegistered -> Customer.fromEvent(event)
    is CustomerActivated -> state!!.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> state!!.copy(isActive = false, reason = event.reason)
  }
}

class CustomerAlreadyExists(val id: UUID) : IllegalStateException("Customer $id already exists")

class CustomerCommandHandler : CommandHandler<Customer, CustomerCommand, CustomerEvent>(customerEventHandler) {
  override fun handleCommand(command: CustomerCommand, state: Customer?): FeatureSession<Customer, CustomerEvent> {
    return when (command) {
      is RegisterCustomer -> {
        if (state != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
      }
      is RegisterAndActivateCustomer -> {
        if (state != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
          .execute { it.activate(command.reason) }
      }
      is ActivateCustomer -> {
        if (command.reason == "because I want it")
          throw IllegalArgumentException("Reason cannot be = [${command.reason}], please be polite.")
        with(state)
          .execute { it.activate(command.reason) }
      }
      is DeactivateCustomer -> {
        with(state)
          .execute { it.deactivate(command.reason) }
      }
    }
  }
}

val customerComponent = FeatureComponent(
  Customer::class,
  CustomerCommand::class,
  CustomerEvent::class,
  customerEventHandler,
  { CustomerCommandHandler() },
  customerCmdValidator
)