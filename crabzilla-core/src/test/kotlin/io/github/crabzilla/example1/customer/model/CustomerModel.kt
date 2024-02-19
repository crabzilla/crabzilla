package io.github.crabzilla.example1.customer.model

import io.github.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RenameCustomer
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRenamed
import java.time.LocalDateTime
import java.util.*

sealed interface CustomerEvent {
  data class CustomerRegistered(val id: UUID, val name: String, val date: LocalDateTime) : CustomerEvent

  data class CustomerActivated(val reason: String) : CustomerEvent

  data class CustomerDeactivated(val reason: String) : CustomerEvent

  data class CustomerRenamed(val name: String) : CustomerEvent
}

sealed interface CustomerCommand {
  data class RegisterCustomer(val customerId: UUID, val name: String) : CustomerCommand

  data class RenameCustomer(val name: String) : CustomerCommand

  data class ActivateCustomer(val reason: String) : CustomerCommand

  data class DeactivateCustomer(val reason: String) : CustomerCommand

  data class RegisterAndActivateCustomer(
    val customerId: UUID,
    val name: String,
    val reason: String,
  ) : CustomerCommand
}

sealed class Customer {
  var timeGenerator: (() -> LocalDateTime) = { LocalDateTime.now() }

  data object Initial : Customer() {
    fun register(
      id: UUID,
      name: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name, date = timeGenerator.invoke()))
    }

    fun registerAndActivate(
      id: UUID,
      name: String,
      reason: String,
    ): List<CustomerEvent> {
      return listOf(
        CustomerRegistered(id = id, name = name, date = timeGenerator.invoke()),
        CustomerActivated(reason = reason),
      )
    }
  }

  interface CustomerProfile {
    fun rename(name: String): List<CustomerRenamed> = listOf(CustomerRenamed(name))
  }

  data class Active(val id: UUID, val name: String, val reason: String) : Customer(), CustomerProfile {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }

    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }

  data class Inactive(val id: UUID, val name: String, val reason: String? = null) : Customer(), CustomerProfile {
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

val customerEvolveFunction: (Customer, CustomerEvent) -> Customer = { state: Customer, event: CustomerEvent ->
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

val customerDecideFunction: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
  when (state) {
    is Customer.Initial -> {
      when (command) {
        is RegisterCustomer ->
          state.register(id = command.customerId, name = command.name)
        is RegisterAndActivateCustomer ->
          state.registerAndActivate(id = command.customerId, name = command.name, reason = command.reason)
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

fun <S, C> buildException(
  state: S,
  command: C,
): IllegalStateException {
  return IllegalStateException(
    "Illegal transition. " +
      "state: ${state!!::class.java.simpleName} command: ${command!!::class.java.simpleName}",
  )
}
