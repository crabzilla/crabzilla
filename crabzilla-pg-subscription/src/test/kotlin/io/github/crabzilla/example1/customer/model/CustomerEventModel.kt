package io.github.crabzilla.example1.customer.model

import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRenamed
import java.util.*

sealed interface CustomerEvent {
  data class CustomerRegistered(val id: UUID, val name: String) : CustomerEvent

  data class CustomerActivated(val reason: String) : CustomerEvent

  data class CustomerDeactivated(val reason: String) : CustomerEvent

  data class CustomerRenamed(val name: String) : CustomerEvent
}

sealed class Customer {
  data object Initial : Customer() {
    fun register(
      id: UUID,
      name: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }

    fun registerAndActivate(
      id: UUID,
      name: String,
      reason: String,
    ): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name), CustomerActivated(reason = reason))
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
