package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityCommandResult
import io.github.crabzilla.core.entity.Snapshot
import io.github.crabzilla.core.entity.StateTransitionsTrackerFactory
import io.github.crabzilla.core.exceptions.UnknownCommandException
import io.github.crabzilla.example1.resultOf
import io.github.crabzilla.example1.uowOf
import java.util.function.BiFunction
import java.util.function.Function

// tag::StateTransitionFn[]

class StateTransitionFn : BiFunction<DomainEvent, Customer, Customer> {
  override fun apply(event: DomainEvent, customer: Customer): Customer {
    return when(event) {
      is CustomerCreated -> customer.copy(_id = event.id, name =  event.name)
      is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
      is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
      else -> customer
    }
  }
}

// end::StateTransitionFn[]

// tag::CommandValidatorFn[]

class CommandValidatorFn : Function<EntityCommand, List<String>> { // <3>
  override fun apply(command: EntityCommand): List<String> {
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

class CommandHandlerFn(private val trackerFactory: StateTransitionsTrackerFactory<Customer>) :
        BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult> {

  override fun apply(cmd: EntityCommand, snapshot: Snapshot<Customer>): EntityCommandResult {

    val customer = snapshot.instance
    val newVersion = snapshot.version.nextVersion()

    return resultOf {
      when (cmd) {
        is CreateCustomer ->
          uowOf(cmd, customer.create(cmd.targetId as CustomerId, cmd.name), newVersion)
        is ActivateCustomer -> uowOf(cmd, customer.activate(cmd.reason), newVersion)
        is DeactivateCustomer -> uowOf(cmd, customer.deactivate(cmd.reason), newVersion)
        is CreateActivateCustomer -> {
          val tracker = trackerFactory.apply(snapshot)
          val events = tracker
                  .applyEvents({ c -> c.create(cmd.targetId as CustomerId, cmd.name) })
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