package io.github.crabzilla

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import java.io.Serializable
import java.util.*
import java.util.regex.Pattern

interface Entity

// schema

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface DomainEvent : Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface EntityId : Serializable {
  fun value(): Int
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface Command : Serializable {
  val commandId: UUID
  val targetId: EntityId
}

typealias Version = Int

data class UnitOfWork(val unitOfWorkId: UUID, val command: Command,
                      val version: Version, val events: List<DomainEvent>) : Serializable {
  init {
    require(this.version >= 1) { "version must be >= 1" }
  }

  fun targetId(): EntityId {
    return command.targetId
  }

}

// command handling helper functions

fun cmdResultOf(f: () -> UnitOfWork): CommandResult {
  return try {
    CommandResult.success(f.invoke())
  }
  catch (e: Exception) {
    CommandResult.error(e)
  }
}

fun uowOf(command: Command, events: List<DomainEvent>, currentVersion: Version): UnitOfWork {
  return UnitOfWork(UUID.randomUUID(), command, currentVersion + 1, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}

// json serialization functions

val eventsListType = object : TypeReference<List<DomainEvent>>() {}

fun listOfEventsFromJson(eventsAsJson: String): List<DomainEvent> {
  return Json.mapper.readerFor(eventsListType).readValue(eventsAsJson)
}

fun listOfEventsToJson(events: List<DomainEvent>): String {
  return Json.mapper.writerFor(eventsListType).writeValueAsString(events)
}

fun commandFromJson(command: String): Command {
  return Json.mapper.readerFor(Command::class.java).readValue(command)
}

fun commandToJson(command: Command): String {
  return Json.mapper.writerFor(Command::class.java).writeValueAsString(command)
}

// vertx

fun initVertx(vertx: Vertx) {

  Json.mapper.registerModule(ParameterNamesModule())
    .registerModule(Jdk8Module())
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule())
    .enable(SerializationFeature.INDENT_OUTPUT)

  //    Json.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

  vertx.eventBus().registerDefaultCodec(ProjectionData::class.java,
    JacksonGenericCodec(Json.mapper, ProjectionData::class.java))

  vertx.eventBus().registerDefaultCodec(CommandExecution::class.java,
    JacksonGenericCodec(Json.mapper, CommandExecution::class.java))

  vertx.eventBus().registerDefaultCodec(EntityId::class.java,
    JacksonGenericCodec(Json.mapper, EntityId::class.java))

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

private fun camelCaseToSpinalCase(start: String): String {
  val m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start)
  val sb = StringBuffer()
  while (m.find()) {
    m.appendReplacement(sb, "-" + m.group())
  }
  m.appendTail(sb)
  return sb.toString().toLowerCase()
}

