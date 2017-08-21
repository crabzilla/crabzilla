package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.resultOf
import io.github.crabzilla.example1.uowOf
import io.github.crabzilla.model.*
import io.github.crabzilla.stack.UnknownCommandException
import java.util.function.BiFunction
import java.util.function.Function

class StateTransitionFn : BiFunction<DomainEvent, Customer, Customer> {
  override fun apply(event: DomainEvent, customer: Customer): Customer {
    return when(event) {
      is CustomerCreated -> customer.copy(_id = event.id, name =  event.name)
      is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
      is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
      else -> customer
    }
  } // <2>
}

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

class CommandHandlerFn(private val trackerFactory: StateTransitionsTrackerFactory<Customer>) :
        BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {

  override fun apply(cmd: EntityCommand, snapshot: Snapshot<Customer>): CommandHandlerResult {

    val customer = snapshot.instance
    val newVersion = snapshot.version.nextVersion()

    return resultOf {
      when (cmd) {
        is CreateCustomer -> uowOf(cmd, customer.create(cmd.targetId as CustomerId, cmd.name), newVersion)
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
