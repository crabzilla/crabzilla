package io.github.crabzilla.example1

import io.github.crabzilla.*

import java.time.Instant
import java.util.*

// TODO replace with inline classes
data class CustomerId(val id: Int) : EntityId {
  override fun value(): Int {
    return id
  }
}

// events

data class CustomerCreated(val id: CustomerId, val name: String) : DomainEvent

data class CustomerActivated(val reason: String, val _when: Instant) : DomainEvent

data class CustomerDeactivated(val reason: String, val _when: Instant) : DomainEvent

// commands

data class CreateCustomer(override val commandId: UUID, override val targetId: CustomerId, val name: String) : Command

data class ActivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val reason: String)
  : Command

data class DeactivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val reason: String)
  : Command

data class CreateActivateCustomer(override val commandId: UUID, override val targetId: CustomerId, val name: String,
                                  val reason: String) : Command

// just for test

data class UnknownCommand(override val commandId: UUID, override val targetId: CustomerId) : Command

// aggregate root

data class Customer(val customerId: CustomerId? = null,
                    val name: String? = null,
                    val isActive: Boolean? = false,
                    val reason: String? = null,
                    val pojoService: PojoService) : Entity {

  override val id: EntityId?
    get() = customerId

  internal fun create(id: CustomerId, name: String): List<DomainEvent> {
    require(this.customerId == null) { "customer already created" }
    return eventsOf(CustomerCreated(id, name))
  }

  internal fun activate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerActivated(reason, pojoService.now()))
  }

  internal fun deactivate(reason: String): List<DomainEvent> {
    customerMustExist()
    return eventsOf(CustomerDeactivated(reason, pojoService.now()))
  }

  private fun customerMustExist() {
    require(this.customerId != null) { "customer must exists" }
  }

}

val stateTransitionFn = { event: DomainEvent, customer: Customer ->
  when(event) {
    is CustomerCreated -> customer.copy(customerId = event.id, name =  event.name)
    is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
    is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
    else -> customer
}}

val commandValidatorFn = { command: Command ->
    when(command) {
      is CreateCustomer ->
        if (command.name.equals("a bad name"))
          listOf("Invalid name: ${command.name}") else listOf()
      else -> listOf() // all other commands are valid
  }
}

val commandHandlerFn = { cmd: Command, snapshot: Snapshot<Customer> ->
    val customer = snapshot.instance
    resultOf {
      when (cmd) {
        is CreateCustomer -> uowOf(cmd, customer.create(cmd.targetId, cmd.name), snapshot.version)
        is ActivateCustomer -> uowOf(cmd, customer.activate(cmd.reason), snapshot.version)
        is DeactivateCustomer -> uowOf(cmd, customer.deactivate(cmd.reason), snapshot.version)
        is CreateActivateCustomer -> {
          val tracker = StateTransitionsTracker(snapshot, stateTransitionFn)
          val events = tracker
            .applyEvents { c -> c.create(cmd.targetId, cmd.name) }
            .applyEvents { c -> c.activate(cmd.reason) }
            .collectEvents()
          uowOf(cmd, events, snapshot.version)
        }
        else -> null
      }
    }
}
