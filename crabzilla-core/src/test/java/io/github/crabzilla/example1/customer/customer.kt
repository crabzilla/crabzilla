package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandValidator
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.StatefulSession
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.util.UUID

/**
 * Customer events
 */
@Serializable
sealed class CustomerEvent : Event() {
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
sealed class CustomerCommand : Command() {
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

/**
 * Customer aggregate root
 */
@Serializable
@SerialName("Customer")
data class Customer(
  @Contextual val id: UUID,
  val name: String,
  val isActive: Boolean = false,
  val reason: String? = null
) : State() {

  companion object {
    fun create(id: UUID, name: String): List<CustomerEvent> {
      return listOf(CustomerRegistered(id = id, name = name))
    }
    fun fromEvent(event: CustomerRegistered): Customer {
      return Customer(id = event.id, name = event.name, isActive = false)
    }
  }

  fun activate(reason: String): List<CustomerEvent> {
    return listOf(CustomerActivated(reason))
  }

  fun deactivate(reason: String): List<CustomerEvent> {
    return listOf(CustomerDeactivated(reason))
  }
}

/**
 * A command validator. You could use https://github.com/konform-kt/konform
 */
val customerCmdValidator = CommandValidator<CustomerCommand> { command ->
  when (command) {
    is RegisterCustomer -> if (command.name == "bad customer") listOf("Bad customer!") else listOf()
    is RegisterAndActivateCustomer -> listOf()
    is ActivateCustomer -> listOf()
    is DeactivateCustomer -> listOf()
  }
}

/**
 * This function will apply an event to customer state
 */
val customerEventHandler = EventHandler<Customer, CustomerEvent> { state, event ->
  when (event) {
    is CustomerRegistered -> Customer.fromEvent(event)
    is CustomerActivated -> state!!.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> state!!.copy(isActive = false, reason = event.reason)
  }
}

/**
 * Customer errors
 */
class CustomerAlreadyExists(val id: UUID) : IllegalStateException("Customer $id already exists")

/**
 * Customer command handler
 */
class CustomerCommandHandler :
  CommandHandler<Customer, CustomerCommand, CustomerEvent>(customerEventHandler) {

  override fun handleCommand(
    command: CustomerCommand,
    snapshot: Snapshot<Customer>?
  ): StatefulSession<Customer, CustomerEvent> {

    return when (command) {

      is RegisterCustomer -> {
        if (snapshot != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
      }

      is RegisterAndActivateCustomer -> {
        if (snapshot != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
          .execute { it.activate(command.reason) }
      }

      is ActivateCustomer -> {
        with(snapshot)
          .execute { it.activate(command.reason) }
      }

      is DeactivateCustomer -> {
        with(snapshot)
          .execute { it.deactivate(command.reason) }
      }
    }
  }
}

val customerConfig = CommandControllerConfig(
  "Customer",
  customerEventHandler,
  { CustomerCommandHandler() },
  customerCmdValidator
)

/**
 * kotlinx.serialization
 */
@kotlinx.serialization.ExperimentalSerializationApi
val customerModule = SerializersModule {
  polymorphic(State::class) {
    subclass(Customer::class, Customer.serializer())
  }
  polymorphic(Command::class) {
    subclass(RegisterCustomer::class, RegisterCustomer.serializer())
    subclass(ActivateCustomer::class, ActivateCustomer.serializer())
    subclass(DeactivateCustomer::class, DeactivateCustomer.serializer())
    subclass(RegisterAndActivateCustomer::class, RegisterAndActivateCustomer.serializer())
  }
  polymorphic(Event::class) {
    subclass(CustomerRegistered::class, CustomerRegistered.serializer())
    subclass(CustomerActivated::class, CustomerActivated.serializer())
    subclass(CustomerDeactivated::class, CustomerDeactivated.serializer())
  }
}
