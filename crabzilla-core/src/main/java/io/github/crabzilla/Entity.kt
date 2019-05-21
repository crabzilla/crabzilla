package io.github.crabzilla

interface Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}
