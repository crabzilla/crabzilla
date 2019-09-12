package io.github.crabzilla.framework

import io.vertx.core.Promise

abstract class EntityCommandHandler<E: Entity>(private val entityName: String,
                                               val cmdMetadata: CommandMetadata,
                                               val command: Command,
                                               val snapshot: Snapshot<E>,
                                               val stateFn: (DomainEvent, E) -> E) {

  private val uowPromise: Promise<UnitOfWork> = Promise.promise()

  abstract fun handleCommand() : Promise<UnitOfWork>

  fun fromEvents(eventsPromise: Promise<List<DomainEvent>>): Promise<UnitOfWork> {
    return if (eventsPromise.future().succeeded()) {
      uowPromise.complete(UnitOfWork.of(cmdMetadata.entityId, entityName, cmdMetadata.commandId,
        cmdMetadata.commandName, command, eventsPromise.future().result(), snapshot.version + 1))
      uowPromise
    } else {
      Promise.failedPromise(eventsPromise.future().cause())
    }
  }
}
