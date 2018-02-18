package io.github.crabzilla.core

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

// json serialization functions

val eventsListType = object : TypeReference<List<DomainEvent>>() {}

fun listOfEventsFromJson(mapper: ObjectMapper, eventsAsJson: String): List<DomainEvent> {
  try {
    return mapper.readerFor(eventsListType).readValue(eventsAsJson)
  } catch (e: IOException) {
    throw RuntimeException("When reading events list from JSON", e)
  }

}

fun listOfEventsToJson(mapper: ObjectMapper, events: List<DomainEvent>): String {
  try {
    val cmdAsJson = mapper.writerFor(eventsListType).writeValueAsString(events)
    return cmdAsJson
  } catch (e: JsonProcessingException) {
    throw RuntimeException("When writing listOfEventsToJson", e)
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

