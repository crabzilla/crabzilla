package io.github.crabzilla.example1.customer

import io.github.crabzilla.*

// tag::StateTransitionFn[]

class StateTransitionFn : (DomainEvent, Customer) -> Customer {
  override fun invoke(event: DomainEvent, customer: Customer): Customer {
    return when(event) {
      is CustomerCreated -> customer.copy(customerId = event.id, name =  event.name)
      is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
      is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
      else -> customer
    }
  }
}

// end::StateTransitionFn[]

// tag::CommandValidatorFn[]

class CommandValidatorFn : (Command) -> List<String> {
  override fun invoke(command: Command): List<String> {
    return when(command) {
      is CreateCustomer ->
        if (command.name.equals("a bad name"))
          listOf("Invalid name: ${command.name}") else listOf()
      else -> listOf() // all other commands are valid
    }
  }
}

// end::CommandValidatorFn[]

// tag::CommandHandlerFn[]

class CommandHandlerFn(
        private val trackerFactory: (Snapshot<Customer>) -> StateTransitionsTracker<Customer>) :
        (Command, Snapshot<Customer>) -> CommandResult? {

  override fun invoke(cmd: Command, snapshot: Snapshot<Customer>): CommandResult? {

    val customer = snapshot.instance

    return resultOf {
      when (cmd) {
        is CreateCustomer ->
          uowOf(cmd, customer.create(cmd.targetId, cmd.name), snapshot.version)
        is ActivateCustomer -> uowOf(cmd, customer.activate(cmd.reason), snapshot.version)
        is DeactivateCustomer -> uowOf(cmd, customer.deactivate(cmd.reason), snapshot.version)
        is CreateActivateCustomer -> {
          val tracker = trackerFactory.invoke(snapshot)
          val events = tracker
                  .applyEvents({ c -> c.create(cmd.targetId, cmd.name) })
                  .applyEvents({ c -> c.activate(cmd.reason) })
                  .collectEvents()
          uowOf(cmd, events, snapshot.version)
        }
        else -> null
      }
    }
  }
}

// end::CommandHandlerFn[]
