package io.github.crabzilla.vertx

import io.github.crabzilla.core.EntityCommand
import io.github.crabzilla.core.SnapshotData
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.Version
import io.github.crabzilla.vertx.projection.ProjectionData
import io.vertx.core.AsyncResult
import io.vertx.core.Future
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

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, uowFuture: Future<UnitOfWork>)

  fun getUowByUowId(uowId: UUID, uowFuture: Future<UnitOfWork>)

  operator fun get(querie: String, id: UUID, uowFuture: Future<UnitOfWork>)

  fun selectAfterVersion(id: String, version: Version, selectAfterVersionFuture: Future<SnapshotData>,
                         aggregateRootName: String)

  fun append(unitOfWork: UnitOfWork, appendFuture: Future<Long>, aggregateRootName: String)

  fun selectAfterUowSequence(uowSequence: Long, maxRows: Int,
                             selectAfterUowSeq: Future<List<ProjectionData>>)
}

class DbConcurrencyException(s: String) : RuntimeException(s)
