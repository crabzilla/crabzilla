package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException
import io.github.crabzilla.core.command.CommandException.UnknownCommandException
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.CommandValidator
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.json.javaModule
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

import java.util.UUID

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
  data class UnknownCommand(val x: String) : CustomerCommand()
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
) {

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
    else -> listOf()
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
    state: Customer?
  ): CommandSession<Customer, CustomerEvent> {

    return when (command) {

      is RegisterCustomer -> {
        if (state != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
      }

      is RegisterAndActivateCustomer -> {
        if (state != null) throw CustomerAlreadyExists(command.customerId)
        withNew(Customer.create(id = command.customerId, name = command.name))
          .execute { it.activate(command.reason) }
      }

      is ActivateCustomer -> {
        if (command.reason == "because I want it")
          throw IllegalArgumentException("Reason cannot be = [${command.reason}], please be polite.")
        with(state)
          .execute { it.activate(command.reason) }
      }

      is DeactivateCustomer -> {
        with(state)
          .execute { it.deactivate(command.reason) }
      }
      else -> {
        throw UnknownCommandException(command::class.java.canonicalName)
      }
    }
  }
}

val customerConfig = CommandControllerConfig(
  PolymorphicSerializer(Customer::class),
  PolymorphicSerializer(CustomerCommand::class),
  PolymorphicSerializer(CustomerEvent::class),
  customerEventHandler,
  { CustomerCommandHandler() },
  customerCmdValidator
)

/**
 * kotlinx.serialization
 */
@kotlinx.serialization.ExperimentalSerializationApi
val customerModule = SerializersModule {
  include(javaModule)
  polymorphic(Customer::class) {
    subclass(Customer::class, Customer.serializer())
  }
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

val example1Json = Json { serializersModule = customerModule }