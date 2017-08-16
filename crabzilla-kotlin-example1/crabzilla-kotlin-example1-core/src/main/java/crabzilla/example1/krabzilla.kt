package crabzilla.example1

import crabzilla.example1.customer.CustomerId
import crabzilla.model.*
import java.util.*

interface KotlinEntityCommand : EntityCommand {

  val _commandId: UUID
  val _targetId: CustomerId

  override fun getCommandId(): UUID {
    return _commandId
  }

  override fun getTargetId(): EntityId {
    return _targetId
  }

}

// helpers functions

fun resultOf(f: () -> EntityUnitOfWork?): CommandHandlerResult {
  return try {
    CommandHandlerResult.success(f.invoke()) }
  catch (e: Throwable) {
    CommandHandlerResult.error(RuntimeException(e)) }
}

fun uowOf(command: EntityCommand, events: List<DomainEvent>, version: Version): EntityUnitOfWork {
  return EntityUnitOfWork(UUID.randomUUID(), command, version, events)
}

fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
  return event.asList()
}

