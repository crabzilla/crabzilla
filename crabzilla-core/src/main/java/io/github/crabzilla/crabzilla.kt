package io.github.crabzilla

import java.io.Serializable
import java.util.*

interface Entity

data class CommandExecution(val result: RESULT,
                            val commandId: UUID?,
                            val constraints: List<String> = listOf(),
                            val uowSequence: Int? = 0,
                            val unitOfWork: UnitOfWork? = null) : Serializable {

  enum class RESULT {
    FALLBACK,
    VALIDATION_ERROR,
    HANDLING_ERROR,
    CONCURRENCY_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS
  }

}

class CommandResult private constructor(val unitOfWork: UnitOfWork?,
                                        val exception: Exception?) {

  fun inCaseOfSuccess(uowFn: (UnitOfWork?) -> Unit) {
    if (unitOfWork != null) {
      uowFn.invoke(unitOfWork)
    }
  }

  fun inCaseOfError(uowFn: (Exception) -> Unit) {
    if (exception != null) {
      uowFn.invoke(exception)
    }
  }

  companion object {

    fun success(uow: UnitOfWork?): CommandResult {
      return CommandResult(uow, null)
    }

    fun error(e: Exception): CommandResult {
      return CommandResult(null, e)
    }
  }

}

// command handling helper functions

fun resultOf(f: () -> UnitOfWork?): CommandResult {
  return try {
    CommandResult.success(f.invoke())
  }
  catch (e: Exception) {
    CommandResult.error(e)
  }
}

fun uowOf(command: Command, events: List<DomainEvent>, currentVersion: Version): UnitOfWork {
  return UnitOfWork(UUID.randomUUID(), command, currentVersion + 1, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}
