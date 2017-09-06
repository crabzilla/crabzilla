package io.github.crabzilla.example1

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.*
import io.github.crabzilla.example1.customer.CustomerId
import java.util.*

interface KotlinEntityCommand : EntityCommand {

  val _commandId: UUID
  val _targetId: CustomerId

  @JsonIgnore
  override fun getCommandId(): UUID {
    return _commandId
  }

  @JsonIgnore
  override fun getTargetId(): EntityId {
    return _targetId
  }

}

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

