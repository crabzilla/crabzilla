package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*

// https://event-driven.io/en/writing_and_testing_business_logic_in_fsharp/

sealed interface CustomerEvent {
  data class CustomerRegistered(val id: String, val name: String) : CustomerEvent
  data class CustomerActivated(val reason: String) : CustomerEvent
  data class CustomerDeactivated(val reason: String) : CustomerEvent
}

sealed interface CustomerCommand {
  data class RegisterCustomer(val customerId: String, val name: String) : CustomerCommand
  data class ActivateCustomer(val reason: String) : CustomerCommand
  data class DeactivateCustomer(val reason: String) : CustomerCommand
  data class RegisterAndActivateCustomer(
    val customerId: String,
    val name: String,
    val reason: String
  ) : CustomerCommand
}

sealed class Customer {
  object Initial: Customer() {
    fun create(id: String, name: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }
    fun createAndActivate(id: String, name: String, reason: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name), CustomerActivated(reason = reason))
    }
  }
  data class Active(val id: String, val name: String, val reason: String): Customer() {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }
    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }
  data class Inactive(val id: String, val name: String, val reason: String? = null): Customer() {
    fun activate(reason: String): List<CustomerEvent> {
      return listOf(CustomerActivated(reason))
    }
    fun toActive(reason: String): Active {
      return Active(id, name, reason)
    }
  }
}

val customerEventHandler = EventHandler<Customer, CustomerEvent> { state, event ->
  when (state) {
    is Customer.Initial -> {
      when (event) {
        is CustomerRegistered -> Customer.Inactive(id = event.id, name = event.name)
        else -> state
      }
    }
    is Customer.Inactive -> {
      when (event) {
        is CustomerActivated -> state.toActive(reason = event.reason)
        else -> state
      }
    }
    is Customer.Active -> {
      when (event) {
        is CustomerDeactivated -> state.toInactive(reason = event.reason)
        else -> state
      }
    }
  }
}

class CustomerAlreadyExists(id: String) : IllegalStateException("Customer $id already exists")

class CustomerCommandHandler
  : CommandHandler<Customer, CustomerCommand, CustomerEvent>(applier = customerEventHandler) {

  override fun handle(command: CustomerCommand, state: Customer): CommandSession<Customer, CustomerEvent> {
    return when (command) {
      is RegisterCustomer -> {
        when (state) {
          is Customer.Initial -> with(state).execute {
            state.create(id = command.customerId, name = command.name)
          }
          else -> throw CustomerAlreadyExists(command.customerId)
        }
      }
      is RegisterAndActivateCustomer -> {
        when (state) {
          is Customer.Initial -> with(state).execute {
            state.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
          }
          else -> throw CustomerAlreadyExists(command.customerId)
        }
      }
      is ActivateCustomer -> {
        when (state) {
          is Customer.Inactive -> with(state).execute {
            if (command.reason == "because I want it")
              throw IllegalArgumentException("Reason cannot be = [${command.reason}], please be polite.")
            state.activate(reason = command.reason)
          }
          else -> throw buildException(state, command)
        }
      }
      is DeactivateCustomer -> {
        when (state) {
          is Customer.Active -> with(state).execute {
            state.deactivate(reason = command.reason)
          }
          else -> throw buildException(state, command)
        }
      }
    }
  }

}
