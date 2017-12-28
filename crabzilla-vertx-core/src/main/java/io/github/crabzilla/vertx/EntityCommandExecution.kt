package io.github.crabzilla.vertx

import io.github.crabzilla.core.entity.EntityUnitOfWork
import java.io.Serializable
import java.util.*

data class EntityCommandExecution(val result: RESULT,
                                  val commandId: UUID?,
                                  val constraints: List<String?> = listOf(),
                                  val uowSequence: Long? = 0L,
                                  val unitOfWork: EntityUnitOfWork? = null) : Serializable {

  enum class RESULT {
    FALLBACK,
    VALIDATION_ERROR,
    HANDLING_ERROR,
    CONCURRENCY_ERROR,
    UNKNOWN_COMMAND,
    SUCCESS
  }

}
