package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
import io.github.crabzilla.kotlinx.json.javaModule
import io.github.crabzilla.stack.command.CommandServiceConfig
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.util.*

/**
 * Customer events
 */
@Serializable
sealed class CustomerEvent {
  @Serializable
  @SerialName("CustomerRegistered")
  data class CustomerRegistered(@Contextual val id: UUID, val name: String) : CustomerEvent()

  @Serializable
  @SerialName("CustomerActivated")
  data class CustomerActivated(val reason: String) : CustomerEvent()

  @Serializable
  @SerialName("CustomerDeactivated")
  data class CustomerDeactivated(val reason: String) : CustomerEvent()
}

/**
 * Customer commands
 */
@Serializable
sealed class CustomerCommand {
  @Serializable
  @SerialName("RegisterCustomer")
  data class RegisterCustomer(@Contextual val customerId: UUID, val name: String) : CustomerCommand()

  @Serializable
  @SerialName("ActivateCustomer")
  data class ActivateCustomer(val reason: String) : CustomerCommand()

  @Serializable
  @SerialName("DeactivateCustomer")
  data class DeactivateCustomer(val reason: String) : CustomerCommand()

  @Serializable
  @SerialName("RegisterAndActivateCustomer")
  data class RegisterAndActivateCustomer(
    @Contextual val customerId: UUID,
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

val customerConfig = CommandServiceConfig(
  Customer::class,
  CustomerCommand::class,
  CustomerEvent::class,
  customerEventHandler,
  CustomerCommandHandler(),
  Customer.Initial
)


@kotlinx.serialization.ExperimentalSerializationApi
val customerModule = SerializersModule {
  include(javaModule)
  polymorphic(CustomerCommand::class) {
    subclass(RegisterCustomer::class, RegisterCustomer.serializer())
    subclass(ActivateCustomer::class, ActivateCustomer.serializer())
    subclass(DeactivateCustomer::class, DeactivateCustomer.serializer())
    subclass(RegisterAndActivateCustomer::class, RegisterAndActivateCustomer.serializer())
  }
  polymorphic(CustomerEvent::class) {
    subclass(CustomerRegistered::class, CustomerRegistered.serializer())
    subclass(CustomerActivated::class, CustomerActivated.serializer())
    subclass(CustomerDeactivated::class, CustomerDeactivated.serializer())
  }
}
