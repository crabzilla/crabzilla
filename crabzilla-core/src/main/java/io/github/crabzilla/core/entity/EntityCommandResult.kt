package io.github.crabzilla.core.entity

class EntityCommandResult private constructor(private val unitOfWork: EntityUnitOfWork?,
                                              private val exception: Throwable?) {

  fun inCaseOfSuccess(uowFn: (EntityUnitOfWork?) -> Unit) {
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

    fun success(uow: EntityUnitOfWork): EntityCommandResult {
      return EntityCommandResult(uow, null)
    }

    fun error(e: Throwable): EntityCommandResult {
      return EntityCommandResult(null, e)
    }
  }

}
