package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnknownCommandException
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityCommandResult
import io.github.crabzilla.core.entity.Snapshot
import io.github.crabzilla.core.entity.StateTransitionsTracker
import io.github.crabzilla.core.resultOf
import io.github.crabzilla.core.uowOf

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

class CommandValidatorFn : (EntityCommand) -> List<String> {
  override fun invoke(command: EntityCommand): List<String> {
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
        (EntityCommand, Snapshot<Customer>) -> EntityCommandResult {

  override fun invoke(cmd: EntityCommand, snapshot: Snapshot<Customer>): EntityCommandResult {

    val customer = snapshot.instance
    val newVersion = snapshot.version.nextVersion()

    return resultOf {
      when (cmd) {
        is CreateCustomer ->
          uowOf(cmd, customer.create(cmd.targetId, cmd.name), newVersion)
        is ActivateCustomer -> uowOf(cmd, customer.activate(cmd.reason), newVersion)
        is DeactivateCustomer -> uowOf(cmd, customer.deactivate(cmd.reason), newVersion)
        is CreateActivateCustomer -> {
          val tracker = trackerFactory.invoke(snapshot)
          val events = tracker
                  .applyEvents({ c -> c.create(cmd.targetId, cmd.name) })
                  .applyEvents({ c -> c.activate(cmd.reason) })
                  .collectEvents()
          uowOf(cmd, events, newVersion)
        }
        else -> throw UnknownCommandException("for command ${cmd.javaClass.simpleName}")
      }
    }
  }
}

// end::CommandHandlerFn[]
