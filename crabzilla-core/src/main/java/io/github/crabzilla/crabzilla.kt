package io.github.crabzilla

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.github.crabzilla.JsonMetadata.EVENTS_JSON_CONTENT
import io.github.crabzilla.JsonMetadata.EVENT_NAME
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*
import java.util.regex.Pattern

// schema

interface DomainEvent

interface Command

typealias Version = Int

data class UnitOfWork(val unitOfWorkId: UUID,
                      val targetName: String,
                      val targetId: Int,
                      val commandId: UUID,
                      val commandName: String,
                      val command: Command,
                      val version: Version,
                      val events: List<DomainEvent>) {

  init { require(this.version >= 1) { "version must be >= 1" } }

  companion object {

    fun of(targetId: Int, targetName: String, commandId: UUID, commandName: String, command: Command,
           events: List<DomainEvent>, resultingVersion: Version): UnitOfWork {

      return UnitOfWork(UUID.randomUUID(), targetName, targetId, commandId, commandName, command, resultingVersion,
              events)
    }
  }
}

// runtime

interface Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}

data class Snapshot<A : Entity>(val instance: A, val version: Version)

data class SnapshotData(val version: Version, val events: List<DomainEvent>)

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val targetId: Int, val events: List<DomainEvent>) {

  companion object {

    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {

      return ProjectionData(UUID.randomUUID(), uowSequence, uow.targetId, uow.events)
    }
  }
}

// vertx

fun initVertx(vertx: Vertx) {

  Json.mapper
    .registerModule(Jdk8Module())
//    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

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

// endpoints

internal const val COMMAND_HANDLER = "-cmd-handler"
internal const val EVENTS_HANDLER = "-events-handler"

fun restEndpoint(name: String): String {
  return camelCaseToSpinalCase(name)
}

fun cmdHandlerEndpoint(name: String): String {
  return camelCaseToSpinalCase(name) + COMMAND_HANDLER
}

fun projectorEndpoint(bcName: String): String {
  return camelCaseToSpinalCase(bcName) + EVENTS_HANDLER
}

private fun camelCaseToSpinalCase(text: String): String {
  val m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text)
  val sb = StringBuffer()
  while (m.find()) { m.appendReplacement(sb, "-" + m.group()) }
  m.appendTail(sb)
  return sb.toString().toLowerCase()
}

// extensions

fun List<DomainEvent>.toJsonArray(eventToJson: (DomainEvent) -> String): JsonArray {
  val eventsJsonArray = JsonArray()
  this.map { event -> Pair(event.javaClass.simpleName, event) }
    .map { pair -> Pair(pair.first, eventToJson.invoke(pair.second)) }
//    .map { pair -> println(pair.first); println(pair.second); pair}
    .map { pair -> JsonObject().put(EVENT_NAME, pair.first).put(EVENTS_JSON_CONTENT, JsonObject(pair.second))}
    .forEach { jo -> eventsJsonArray.add(jo) }
  return eventsJsonArray
}

