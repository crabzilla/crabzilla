package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RenameCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRenamed

sealed interface CustomerEvent {
  data class CustomerRegistered(val id: String, val name: String) : CustomerEvent

  data class CustomerRenamed(val name: String) : CustomerEvent

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
      return listOf(CustomerRegistered(id = id, name = name), CustomerActivated(reason = reason))
    }
  }

  interface CustomerProfile {
    fun rename(name: String): List<CustomerRenamed> = listOf(CustomerRenamed(name))
  }

  data class Active(val id: String, val name: String, val reason: String) : Customer(), CustomerProfile {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }

    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }

  data class Inactive(val id: String, val name: String, val reason: String? = null) : Customer(), CustomerProfile {
    fun activate(reason: String): List<CustomerEvent> {
      if (reason == "because I want it") {
        throw IllegalArgumentException("Reason cannot be = [$reason], please be polite.")
      }
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
  } else if (state is Customer.Active && event is CustomerRenamed) {
    state.copy(name = event.name)
  } else if (state is Customer.Inactive && event is CustomerRenamed) {
    state.copy(name = event.name)
  } else {
    state
  }
}

sealed interface CustomerCommand {
  data class RegisterCustomer(val customerId: String, val name: String) : CustomerCommand

  data class RenameCustomer(val name: String) : CustomerCommand

  data class ActivateCustomer(val reason: String) : CustomerCommand

  data class DeactivateCustomer(val reason: String) : CustomerCommand

  data class RegisterAndActivateCustomer(
    val customerId: String,
    val name: String,
    val reason: String,
  ) : CustomerCommand
}

val customerCommandHandler: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
  if (state is Customer.Initial && command is RegisterCustomer) {
    state.create(id = command.customerId, name = command.name)
  } else if (state is Customer.CustomerProfile && command is RenameCustomer) {
    state.rename(command.name)
  } else if (state is Customer.Initial && command is RegisterAndActivateCustomer) {
    state.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
  } else if (state is Customer.Inactive && command is ActivateCustomer) {
    state.activate(reason = command.reason)
  } else if (state is Customer.Active && command is DeactivateCustomer) {
    state.deactivate(reason = command.reason)
  } else {
    throw IllegalStateException(
      "Illegal transition. " +
        "state: ${state::class.java.simpleName} command: ${command::class.java.simpleName}",
    )
  }
}
