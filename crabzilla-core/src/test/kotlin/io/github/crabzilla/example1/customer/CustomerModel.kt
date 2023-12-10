package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.buildException
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
  when (state) {
    is Customer.Initial -> {
      when (event) {
        is CustomerRegistered -> Customer.Inactive(id = event.id, name = event.name)
        else -> state
      }
    }
    is Customer.Active -> {
      when (event) {
        is CustomerDeactivated -> state.toInactive(event.reason)
        is CustomerRenamed -> state.copy(name = event.name)
        else -> state
      }
    }
    is Customer.Inactive -> {
      when (event) {
        is CustomerActivated -> state.toActive(reason = event.reason)
        is CustomerRenamed -> state.copy(name = event.name)
        else -> state
      }
    }
  }
}

val customerCommandHandler: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
  when (state) {
    is Customer.Initial -> {
      when (command) {
        is RegisterCustomer ->
          state.create(id = command.customerId, name = command.name)
        is RegisterAndActivateCustomer ->
          state.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
        else -> throw buildException(state, command)
      }
    }
    is Customer.Active -> {
      when (command) {
        is DeactivateCustomer -> state.deactivate(reason = command.reason)
        is RenameCustomer -> state.rename(command.name)
        else -> throw buildException(state, command)
      }
    }
    is Customer.Inactive -> {
      when (command) {
        is ActivateCustomer -> {
          state.activate(reason = command.reason)
        }
        is RenameCustomer -> state.rename(command.name)
        else -> throw buildException(state, command)
      }
    }
  }
}
