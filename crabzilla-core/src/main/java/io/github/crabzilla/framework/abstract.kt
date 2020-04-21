package io.github.crabzilla.framework

import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
open class Command

@Serializable
@Polymorphic
open class DomainEvent

@Serializable
@Polymorphic
open class Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}

interface EntityCommandAware<E: Entity> {
  val initialState: E
  val applyEvent: (event: DomainEvent, state: E) -> E
  val validateCmd: (command: Command) -> List<String>
  val cmdHandlerFactory:
    (cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<E>) -> EntityCommandHandler<E>
}

abstract class EntityCommandHandler<E: Entity>(private val entityName: String, val cmdMetadata: CommandMetadata,
                                               val command: Command, val snapshot: Snapshot<E>,
                                               val stateFn: (DomainEvent, E) -> E) {
  private val uowPromise: Promise<UnitOfWork> = Promise.promise()
  abstract fun handleCommand() : Future<UnitOfWork>
  fun fromEvents(eventsPromise: Future<List<DomainEvent>>): Future<UnitOfWork> {
    return if (eventsPromise.succeeded()) {
      uowPromise.complete(UnitOfWork.of(cmdMetadata.entityId, entityName, cmdMetadata.commandId,
        command, eventsPromise.result(), snapshot.version + 1))
      uowPromise.future()
    } else {
      val promise = Promise.promise<UnitOfWork>()
      promise.fail(eventsPromise.cause())
      return promise.future()
    }
  }
}
