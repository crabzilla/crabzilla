package io.github.crabzilla.example1

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.AggregateRootName
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandHandler.ConstructorResult
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.SnapshotTableName
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Customer events
 */
@Serializable
sealed class CustomerEvent : DomainEvent() {
  @Serializable
  @SerialName("CustomerRegistered")
  data class CustomerRegistered(val id: Int, val name: String) : CustomerEvent()

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
  data class RegisterCustomer(val customerId: Int, val name: String) : CustomerCommand()

  @Serializable
  @SerialName("ActivateCustomer")
  data class ActivateCustomer(val reason: String) : CustomerCommand()

  @Serializable
  @SerialName("DeactivateCustomer")
  data class DeactivateCustomer(val reason: String) : CustomerCommand()

  @Serializable
  @SerialName("RegisterAndActivateCustomer")
  data class RegisterAndActivateCustomer(val customerId: Int, val name: String, val reason: String) : CustomerCommand()
}

/**
 * Customer aggregate root
 */
@Serializable
@SerialName("Customer")
data class Customer(
  val id: Int,
  val name: String,
  val isActive: Boolean = false,
  val reason: String? = null
) : AggregateRoot() {

  companion object {
    fun create(id: Int, name: String): ConstructorResult<Customer, CustomerEvent> {
      return ConstructorResult(Customer(id = id, name = name), CustomerRegistered(id = id, name = name))
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
class CustomerAlreadyExists(val id: Int) : IllegalStateException("Customer $id already exists")

/**
 * Customer command handler
 */
object CustomerCommandHandler : CommandHandler<Customer, CustomerCommand, CustomerEvent> {
  override fun handleCommand(command: CustomerCommand, snapshot: Snapshot<Customer>?):
    Result<StatefulSession<Customer, CustomerEvent>> {

      return runCatching {
        when (command) {

          is RegisterCustomer -> {
            if (snapshot == null)
              with(Customer.create(id = command.customerId, name = command.name), customerEventHandler)
            else throw CustomerAlreadyExists(command.customerId)
          }

          is RegisterAndActivateCustomer -> {
            if (snapshot == null)
              with(Customer.create(id = command.customerId, name = command.name), customerEventHandler)
                .execute { it.activate(command.reason) }
            else throw CustomerAlreadyExists(command.customerId)
          }

          is ActivateCustomer -> {
            with(snapshot!!, customerEventHandler)
              .execute { it.activate(command.reason) }
          }

          is DeactivateCustomer -> {
            with(snapshot!!, customerEventHandler)
              .execute { it.deactivate(command.reason) }
          }
        }
      }
    }
}

// TODO class CustomerEventDes : EventDeserializer<IntegrationEvent>

val customerConfig = AggregateRootConfig(
  AggregateRootName("Customer"),
  SnapshotTableName("customer_snapshots"),
  customerEventHandler,
  customerCmdValidator,
  CustomerCommandHandler,
  customerJson
)
