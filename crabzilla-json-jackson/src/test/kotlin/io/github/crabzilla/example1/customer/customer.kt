package io.github.crabzilla.example1.customer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
import io.github.crabzilla.stack.command.CommandServiceConfig
import ulid4j.Ulid
import java.util.*


/**
 * Customer events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(CustomerRegistered::class, name = "CustomerRegistered"),
  JsonSubTypes.Type(CustomerActivated::class, name = "CustomerActivated"),
  JsonSubTypes.Type(CustomerDeactivated::class, name = "CustomerDeactivated")
)
sealed class CustomerEvent {
  data class CustomerRegistered(val id: UUID, val name: String) : CustomerEvent()
  data class CustomerActivated(val reason: String) : CustomerEvent()
  data class CustomerDeactivated(val reason: String) : CustomerEvent()
}

/**
 * Customer commands
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(RegisterCustomer::class, name = "RegisterCustomer"),
  JsonSubTypes.Type(ActivateCustomer::class, name = "ActivateCustomer"),
  JsonSubTypes.Type(ActivateCustomer::class, name = "DeactivateCustomer"),
  JsonSubTypes.Type(RegisterAndActivateCustomer::class, name = "RegisterAndActivateCustomer")
)
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

sealed class Customer {
  object Initial: Customer() {
    fun create(id: UUID, name: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }
    fun createAndActivate(id: UUID, name: String, reason: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name), CustomerActivated(reason = reason))
    }
  }
  data class Active(val id: UUID, val name: String, val reason: String): Customer() {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }
    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }
  data class Inactive(val id: UUID, val name: String, val reason: String? = null): Customer() {
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

class CustomerAlreadyExists(id: UUID) : IllegalStateException("Customer $id already exists")

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

val ulidGenerator = Ulid()

val customerComponent = CommandServiceConfig(
  Customer::class,
  CustomerCommand::class,
  CustomerEvent::class,
  customerEventHandler,
  CustomerCommandHandler(),
  Customer.Initial
)
