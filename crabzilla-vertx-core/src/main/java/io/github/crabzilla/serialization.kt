package io.github.crabzilla

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

// json serialization functions

val eventsListType = object : TypeReference<List<DomainEvent>>() {}

fun listOfEventsFromJson(mapper: ObjectMapper, eventsAsJson: String): List<DomainEvent> {
    return mapper.readerFor(eventsListType).readValue(eventsAsJson)
}

fun listOfEventsToJson(mapper: ObjectMapper, events: List<DomainEvent>): String {
    return mapper.writerFor(eventsListType).writeValueAsString(events)
}

fun commandToJson(mapper: ObjectMapper, command: Command): String {
    return mapper.writerFor(Command::class.java).writeValueAsString(command)
}

