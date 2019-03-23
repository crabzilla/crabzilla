package io.github.crabzilla

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
