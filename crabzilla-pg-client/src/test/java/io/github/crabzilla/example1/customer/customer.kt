package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandHandlerApi.ConstructorResult
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
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
sealed class CustomerEvent : DomainEvent() {
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
) : DomainState() {

  companion object {
    fun create(id: UUID, name: String): ConstructorResult<Customer, CustomerEvent> {
      return ConstructorResult(
        Customer(id = id, name = name),
        CustomerRegistered(id = id, name = name)
      )
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
    is RegisterCustomer -> listOf()
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
    is CustomerRegistered -> Customer.create(id = event.id, name = event.name).state
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
object CustomerCommandHandler : CommandHandler<Customer, CustomerCommand, CustomerEvent> {
  override fun handleCommand(
    command: CustomerCommand,
    eventHandler: EventHandler<Customer, CustomerEvent>,
    snapshot: Snapshot<Customer>?
  ):
    StatefulSession<Customer, CustomerEvent> {

    return when (command) {

      is RegisterCustomer -> {
        if (snapshot == null)
          withNew(Customer.create(id = command.customerId, name = command.name), eventHandler)
        else throw CustomerAlreadyExists(command.customerId)
      }

      is RegisterAndActivateCustomer -> {
        if (snapshot == null)
          withNew(Customer.create(id = command.customerId, name = command.name), eventHandler)
            .execute { it.activate(command.reason) }
        else throw CustomerAlreadyExists(command.customerId)
      }

      is ActivateCustomer -> {
        with(snapshot!!, eventHandler)
          .execute { it.activate(command.reason) }
      }

      is DeactivateCustomer -> {
        with(snapshot!!, eventHandler)
          .execute { it.deactivate(command.reason) }
      }
    }
  }
}

val customerConfig = CommandControllerConfig(
  "Customer",
  customerEventHandler,
  { CustomerCommandHandler },
  customerCmdValidator
)

/**
 * kotlinx.serialization
 */
@kotlinx.serialization.ExperimentalSerializationApi
val customerModule = SerializersModule {
  polymorphic(DomainState::class) {
    subclass(Customer::class, Customer.serializer())
  }
  polymorphic(Command::class) {
    subclass(RegisterCustomer::class, RegisterCustomer.serializer())
    subclass(ActivateCustomer::class, ActivateCustomer.serializer())
    subclass(DeactivateCustomer::class, DeactivateCustomer.serializer())
    subclass(RegisterAndActivateCustomer::class, RegisterAndActivateCustomer.serializer())
  }
  polymorphic(DomainEvent::class) {
    subclass(CustomerRegistered::class, CustomerRegistered.serializer())
    subclass(CustomerActivated::class, CustomerActivated.serializer())
    subclass(CustomerDeactivated::class, CustomerDeactivated.serializer())
  }
}
