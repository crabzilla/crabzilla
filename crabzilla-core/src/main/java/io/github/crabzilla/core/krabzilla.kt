package io.github.crabzilla.core

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityCommandResult
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.Version
import java.io.IOException
import java.util.*

// command handling helper functions

fun resultOf(f: () -> EntityUnitOfWork?): EntityCommandResult {
  return try {
    EntityCommandResult.success(f.invoke()) }
  catch (e: RuntimeException) {
    EntityCommandResult.error(e) }
}

fun uowOf(command: EntityCommand, events: List<DomainEvent>, version: Version): EntityUnitOfWork {
  return EntityUnitOfWork(UUID.randomUUID(), command, version, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}

// serialization functions

val eventsListType = object : TypeReference<List<DomainEvent>>() {}

fun listOfEventsToJson(mapper: ObjectMapper, events: List<DomainEvent>): String {
  try {
    val cmdAsJson = mapper.writerFor(eventsListType).writeValueAsString(events)
    return cmdAsJson
  } catch (e: JsonProcessingException) {
    throw RuntimeException("When writing listOfEventsToJson", e)
  }

}

fun listOfEventsFromJson(mapper: ObjectMapper, eventsAsJson: String): List<DomainEvent> {
  try {
    return mapper.readerFor(eventsListType).readValue(eventsAsJson)
  } catch (e: IOException) {
    throw RuntimeException("When reading events list from JSON", e)
  }

}

fun commandToJson(mapper: ObjectMapper, command: Command): String {
  try {
    val cmdAsJson = mapper.writerFor(Command::class.java).writeValueAsString(command)
    return cmdAsJson
  } catch (e: JsonProcessingException) {
    throw RuntimeException("When writing commandToJson", e)
  }

}

