package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered

sealed interface CustomerEvent {
  data class CustomerRegistered(val id: String, val name: String) : CustomerEvent

  data class CustomerActivated(val reason: String) : CustomerEvent

  data class CustomerDeactivated(val reason: String) : CustomerEvent
}

sealed class Customer {
  object Initial : Customer() {
    fun create(
      id: String,
      name: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }

    fun createAndActivate(
      id: String,
      name: String,
      reason: String,
    ): List<CustomerEvent> {
      return listOf(
        CustomerRegistered(id = id, name = name),
        CustomerActivated(reason = reason),
      )
    }
  }

  data class Active(val id: String, val name: String, val reason: String) : Customer() {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }

    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }

  data class Inactive(val id: String, val name: String, val reason: String? = null) : Customer() {
    fun activate(reason: String): List<CustomerEvent> {
      return listOf(CustomerActivated(reason))
    }

    fun toActive(reason: String): Active {
      return Active(id, name, reason)
    }
  }
}

val customerEventHandler: (Customer, CustomerEvent) -> Customer = { state: Customer, event: CustomerEvent ->
  if (state is Customer.Initial && event is CustomerRegistered) {
    Customer.Inactive(id = event.id, name = event.name)
  } else if (state is Customer.Inactive && event is CustomerActivated) {
    state.toActive(reason = event.reason)
  } else if (state is Customer.Active && event is CustomerDeactivated) {
    state.toInactive(reason = event.reason)
  } else {
    state
  }
}

sealed interface CustomerCommand {
  data class RegisterCustomer(val customerId: String, val name: String) : CustomerCommand

  data class ActivateCustomer(val reason: String) : CustomerCommand

  data class DeactivateCustomer(val reason: String) : CustomerCommand

  data class RegisterAndActivateCustomer(
    val customerId: String,
    val name: String,
    val reason: String,
  ) : CustomerCommand
}

val customerCommandHandler: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
  if (command is RegisterCustomer && state is Customer.Initial) {
    Customer.Initial.create(id = command.customerId, name = command.name)
  } else if (command is RegisterAndActivateCustomer && state is Customer.Initial) {
    Customer.Initial.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
  } else if (command is ActivateCustomer && state is Customer.Inactive) {
    if (command.reason == "because I want it") {
      throw IllegalArgumentException("Reason cannot be = [${command.reason}], please be polite.")
    }
    state.activate(reason = command.reason)
  } else if (command is DeactivateCustomer && state is Customer.Active) {
    state.deactivate(reason = command.reason)
  } else {
    throw IllegalStateException(
      "Illegal transition. " +
        "state: ${state::class.java.simpleName} command: ${command::class.java.simpleName}",
    )
  }
}
