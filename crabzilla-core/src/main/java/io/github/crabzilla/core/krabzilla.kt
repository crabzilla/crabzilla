package io.github.crabzilla.core

import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityCommandResult
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.Version
import java.util.*

// helpers functions

fun resultOf(f: () -> EntityUnitOfWork): EntityCommandResult {
  return try {
    EntityCommandResult.success(f.invoke()) }
  catch (e: Throwable) {
    EntityCommandResult.error(RuntimeException(e)) }
}

fun uowOf(command: EntityCommand, events: List<DomainEvent>, version: Version): EntityUnitOfWork {
  return EntityUnitOfWork(UUID.randomUUID(), command, version, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}

