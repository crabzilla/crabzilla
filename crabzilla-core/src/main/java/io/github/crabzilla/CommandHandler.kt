package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import java.util.*

abstract class CommandHandler<E: Entity>(private val entityName: String, val cmdMetadata: CommandMetadata,
                                         val command: Command, val snapshot: Snapshot<E>,
                                         val stateFn: (DomainEvent, E) -> E,
                                         uowHandler: Handler<AsyncResult<UnitOfWork>>) {
  private val uowFuture: Future<UnitOfWork> = Future.future()
  protected val eventsFuture: Future<List<DomainEvent>> = Future.future()
  init {
    uowFuture.setHandler(uowHandler)
    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(UnitOfWork.of(cmdMetadata.entityId, entityName,
          cmdMetadata.commandId ?: UUID.randomUUID(),
          cmdMetadata.commandName, command, event.result(), snapshot.version + 1))
      } else {
        uowFuture.fail(event.cause())
      }
    }
  }
  abstract fun handleCommand()
}
