package io.github.crabzilla.framework

import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

interface Command

interface DomainEvent

interface Entity {

  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }

}

interface EntityCommandAware<E: Entity> {

  val initialState: E

  val applyEvent: (event: DomainEvent, state: E) -> E

  val validateCmd: (command: Command) -> List<String>

  val cmdHandlerFactory: (cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<E>)
                                                        -> EntityCommandHandler<E>

}

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

interface EntityJsonAware<E: Entity> {

  fun fromJson(json: JsonObject): E

  fun toJson(entity: E): JsonObject

  fun cmdFromJson(cmdName: String, json: JsonObject): Command

  fun cmdToJson(cmd: Command): JsonObject

  fun eventFromJson(eventName: String, json: JsonObject): Pair<String, DomainEvent>

  fun eventToJson(event: DomainEvent): JsonObject

  fun toJsonArray(events: List<Pair<String, DomainEvent>>): JsonArray {
    val eventsJsonArray = JsonArray()
    events
      .map { pair -> JsonObject().put(UnitOfWork.JsonMetadata.EVENT_NAME, pair.first)
                                 .put(UnitOfWork.JsonMetadata.EVENTS_JSON_CONTENT, eventToJson(pair.second))}
      .forEach { jo -> eventsJsonArray.add(jo) }
    return eventsJsonArray
  }

}
