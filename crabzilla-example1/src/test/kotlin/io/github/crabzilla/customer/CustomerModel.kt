package io.github.crabzilla.customer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.buildException
import io.github.crabzilla.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.customer.CustomerCommand.RenameCustomer
import io.github.crabzilla.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.customer.CustomerEvent.CustomerRenamed

/**
 * Customer events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(CustomerRegistered::class, name = "CustomerRegistered"),
  JsonSubTypes.Type(CustomerRenamed::class, name = "CustomerRenamed"),
  JsonSubTypes.Type(CustomerActivated::class, name = "CustomerActivated"),
  JsonSubTypes.Type(CustomerDeactivated::class, name = "CustomerDeactivated"),
)
sealed interface CustomerEvent {
  data class CustomerRegistered(val id: String, val name: String) : CustomerEvent

  data class CustomerRenamed(val name: String) : CustomerEvent

  data class CustomerActivated(val reason: String) : CustomerEvent

  data class CustomerDeactivated(val reason: String) : CustomerEvent
}

/**
 * Customer commands
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(RegisterCustomer::class, name = "RegisterCustomer"),
  JsonSubTypes.Type(RenameCustomer::class, name = "RenameCustomer"),
  JsonSubTypes.Type(ActivateCustomer::class, name = "ActivateCustomer"),
  JsonSubTypes.Type(DeactivateCustomer::class, name = "DeactivateCustomer"),
  JsonSubTypes.Type(RegisterAndActivateCustomer::class, name = "RegisterAndActivateCustomer"),
)
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

// val customerEventHandlerStyle1: (Customer, CustomerEvent) -> Customer = { state: Customer, event: CustomerEvent ->
//  if (state is Customer.Initial && event is CustomerRegistered) {
//    Customer.Inactive(id = event.id, name = event.name)
//  } else if (state is Customer.Inactive && event is CustomerActivated) {
//    state.toActive(reason = event.reason)
//  } else if (state is Customer.Active && event is CustomerDeactivated) {
//    state.toInactive(reason = event.reason)
//  } else if (state is Customer.Active && event is CustomerRenamed) {
//    state.copy(name = event.name)
//  } else if (state is Customer.Inactive && event is CustomerRenamed) {
//    state.copy(name = event.name)
//  } else {
//    state
//  }
// }
//
// val customerCommandHandlerStyle1: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
//  if (state is Customer.Initial && command is RegisterCustomer) {
//    state.create(id = command.customerId, name = command.name)
//  }
//  if (state is Customer.Initial && command is RegisterAndActivateCustomer) {
//    state.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
//  }
//  if (state is Customer.Inactive && command is ActivateCustomer) {
//    state.activate(reason = command.reason)
//  }
//  if (state is Customer.Active && command is DeactivateCustomer) {
//    state.deactivate(reason = command.reason)
//  }
//  if (state is Customer.CustomerProfile && command is RenameCustomer) {
//    state.rename(command.name)
//  }
//  throw buildException(state, command)
// }
//
// val customerCommandHandlerStyle2: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
//  when (command) {
//    is RegisterCustomer -> {
//      when (state) {
//        is Customer.Initial ->
//          state.create(id = command.customerId, name = command.name)
//        else -> throw CustomerAlreadyExists(command.customerId)
//      }
//    }
//    is RegisterAndActivateCustomer -> {
//      when (state) {
//        is Customer.Initial ->
//          state.createAndActivate(id = command.customerId, name = command.name, reason = command.reason)
//        else -> throw CustomerAlreadyExists(command.customerId)
//      }
//    }
//    is RenameCustomer ->
//      when (state) {
//        is Customer.CustomerProfile -> state.rename(command.name)
//        else -> throw buildException(state, command)
//      }
//    is ActivateCustomer -> {
//      when (state) {
//        is Customer.Inactive -> {
//          if (command.reason == "because I want it") {
//            throw IllegalArgumentException("Reason cannot be = [${command.reason}], please be polite.")
//          }
//          state.activate(reason = command.reason)
//        }
//        else -> throw buildException(state, command)
//      }
//    }
//    is DeactivateCustomer -> {
//      when (state) {
//        is Customer.Active ->
//          state.deactivate(reason = command.reason)
//        else -> throw buildException(state, command)
//      }
//    }
//  }
// }
//
// class CustomerAlreadyExists(customerId: String) : RuntimeException(customerId)
