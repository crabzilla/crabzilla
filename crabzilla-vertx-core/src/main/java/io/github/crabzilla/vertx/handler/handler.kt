package io.github.crabzilla.vertx.handler

import io.github.crabzilla.core.EntityCommand
import io.github.crabzilla.core.UnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import java.io.Serializable
import java.util.*

data class CommandExecution(val result: RESULT,
                            val commandId: UUID?,
                            val constraints: List<String?> = listOf(),
                            val uowSequence: Long? = 0L,
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

interface CommandHandlerService {

  fun postCommand(handlerEndpoint: String, command: EntityCommand, handler: Handler<AsyncResult<CommandExecution>>)

}
