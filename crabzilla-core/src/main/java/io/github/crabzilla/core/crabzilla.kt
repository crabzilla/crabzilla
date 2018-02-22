package io.github.crabzilla.core

import java.io.Serializable
import java.util.*

interface Entity : Serializable {
  val id: EntityId
}

class CommandResult private constructor(private val unitOfWork: UnitOfWork?,
                                        private val exception: Throwable?) {

  fun inCaseOfSuccess(uowFn: (UnitOfWork?) -> Unit) {
    if (unitOfWork != null) {
      uowFn.invoke(unitOfWork)
    }
  }

  fun inCaseOfError(uowFn: (Throwable) -> Unit) {
    if (exception != null) {
      uowFn.invoke(exception)
    }
  }

  companion object {

    fun success(uow: UnitOfWork?): CommandResult {
      return CommandResult(uow, null)
    }

    fun error(e: Throwable): CommandResult {
      return CommandResult(null, e)
    }
  }

}

// command handling helper functions

fun resultOf(f: () -> UnitOfWork?): CommandResult {
  return try {
    CommandResult.success(f.invoke()) }
  catch (e: RuntimeException) {
    CommandResult.error(e) }
}

fun uowOf(command: Command, events: List<DomainEvent>, currentVersion: Version): UnitOfWork {
  return UnitOfWork(UUID.randomUUID(), command, currentVersion +1, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}

