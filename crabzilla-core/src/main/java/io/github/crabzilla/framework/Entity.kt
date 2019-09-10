package io.github.crabzilla.framework

interface Entity {

  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }

}
