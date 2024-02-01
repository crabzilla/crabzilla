package io.github.crabzilla.example1.customer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import java.util.*

/**
 * Customer events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(CustomerRegistered::class, name = "CustomerRegistered"),
  JsonSubTypes.Type(CustomerActivated::class, name = "CustomerActivated"),
  JsonSubTypes.Type(CustomerDeactivated::class, name = "CustomerDeactivated"),
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
  JsonSubTypes.Type(RegisterAndActivateCustomer::class, name = "RegisterAndActivateCustomer"),
)
sealed class CustomerCommand {
  data class RegisterCustomer(val customerId: UUID, val name: String) : CustomerCommand()

  data class ActivateCustomer(val reason: String) : CustomerCommand()

  data class DeactivateCustomer(val reason: String) : CustomerCommand()

  data class RegisterAndActivateCustomer(
    val customerId: UUID,
    val name: String,
    val reason: String,
  ) : CustomerCommand()
}

sealed class Customer {
  data object Initial : Customer() {
    fun create(
      id: UUID,
      name: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }

    fun createAndActivate(
      id: UUID,
      name: String,
      reason: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name), CustomerActivated(reason = reason))
    }
  }

  data class Active(val id: UUID, val name: String, val reason: String) : Customer() {
    fun deactivate(reason: String): List<CustomerEvent> {
      return listOf(CustomerDeactivated(reason))
    }

    fun toInactive(reason: String): Inactive {
      return Inactive(id, name, reason)
    }
  }

  data class Inactive(val id: UUID, val name: String, val reason: String? = null) : Customer() {
    fun activate(reason: String): List<CustomerEvent> {
      return listOf(CustomerActivated(reason))
    }

    fun toActive(reason: String): Active {
      return Active(id, name, reason)
    }
  }
}
