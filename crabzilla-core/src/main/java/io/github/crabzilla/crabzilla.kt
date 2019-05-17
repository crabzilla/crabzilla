package io.github.crabzilla

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.github.crabzilla.UnitOfWork.JsonMetadata.EVENTS_JSON_CONTENT
import io.github.crabzilla.UnitOfWork.JsonMetadata.EVENT_NAME
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

// schema

interface DomainEvent

interface Command

typealias Version = Int

interface Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}

data class UnitOfWork(val unitOfWorkId: UUID,
                      val entityName: String,
                      val entityId: Int,
                      val commandId: UUID,
                      val commandName: String,
                      val command: Command,
                      val version: Version,
                      val events: List<DomainEvent>) {

  init { require(this.version >= 1) { "version must be >= 1" } }

  companion object {
    fun of(entityId: Int, entityName: String, commandId: UUID, commandName: String, command: Command,
           events: List<DomainEvent>, resultingVersion: Version): UnitOfWork {
      return UnitOfWork(UUID.randomUUID(), entityName, entityId, commandId, commandName, command, resultingVersion,
              events)
    }
  }

  object JsonMetadata {
    const val UOW_ID = "unitOfWorkId"
    const val ENTITY_NAME = "entityName"
    const val ENTITY_ID = "entityId"
    const val COMMAND_ID = "commandId"
    const val COMMAND_NAME = "commandName"
    const val COMMAND = "command"
    const val VERSION = "version"
    const val EVENTS = "events"

    const val EVENT_NAME = "eventName"
    const val EVENTS_JSON_CONTENT = "eventJson"
  }
}

// runtime

data class Snapshot<E : Entity>(val state: E, val version: Version)

data class SnapshotData(val version: Version, val events: List<DomainEvent>)

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val entityId: Int, val events: List<DomainEvent>) {
  companion object {
    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {
      return ProjectionData(uow.unitOfWorkId, uowSequence, uow.entityId, uow.events)
    }
  }
}

// vertx

fun initVertx(vertx: Vertx) {

  Json.mapper
    .registerModule(Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // TODO test this

  vertx.eventBus().registerDefaultCodec(ProjectionData::class.java,
    JacksonGenericCodec(Json.mapper, ProjectionData::class.java))

  vertx.eventBus().registerDefaultCodec(Pair::class.java,
    JacksonGenericCodec(Json.mapper, Pair::class.java))

  vertx.eventBus().registerDefaultCodec(Command::class.java,
    JacksonGenericCodec(Json.mapper, Command::class.java))

  vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
    JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

  vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
    JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

}

// extensions


fun List<DomainEvent>.toJsonArray(eventToJson: (DomainEvent) -> JsonObject): JsonArray {
  val eventsJsonArray = JsonArray()
  this.map { event -> Pair(event.javaClass.simpleName, event) }
    .map { pair -> Pair(pair.first, eventToJson.invoke(pair.second)) }
//    .map { pair -> println(pair.first); println(pair.second); pair}
    .map { pair -> JsonObject().put(EVENT_NAME, pair.first).put(EVENTS_JSON_CONTENT, pair.second)}
    .forEach { jo -> eventsJsonArray.add(jo) }
  return eventsJsonArray
}

// exploring

interface EntitySerDer
