package io.github.crabzilla.internal

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.Version
import io.vertx.core.Future
import io.vertx.core.Promise
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory

data class RangeOfEvents(val afterVersion: Version, val untilVersion: Version, val events: List<DomainEvent>)

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)

fun fromUnitOfWork(uowId: Long, uow: UnitOfWork): UnitOfWorkEvents {
  return UnitOfWorkEvents(uowId, uow.entityId, uow.events)
}

class CommandController<E : Entity>(
  private val commandAware: EntityCommandAware<E>,
  private val snapshotRepo: SnapshotRepository<E>,
  private val uowJournal: UnitOfWorkJournal
) {
  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    if (log.isDebugEnabled) log.debug("received $metadata\n $command")
    val constraints = commandAware.validateCmd(command)
    if (constraints.isNotEmpty()) {
      log.error("Command is invalid: $constraints")
      promise.fail(constraints.toString())
      return promise.future()
    }
    val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
    val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
    val uowIdValue: AtomicReference<Long> = AtomicReference()
    snapshotRepo.retrieve(metadata.entityId)
      .compose { snapshot ->
        if (log.isDebugEnabled) log.debug("got snapshot $snapshot")
        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
        snapshotValue.set(cachedSnapshot)
        val cmdHandler = commandAware.cmdHandlerFactory.invoke(metadata, command, cachedSnapshot)
        val uow = cmdHandler.handleCommand()
        uow
      }
      .compose { unitOfWork ->
        if (log.isDebugEnabled) log.debug("got unitOfWork $unitOfWork")
        // append to journal
        uowValue.set(unitOfWork)
        val uowId = uowJournal.append(unitOfWork)
        uowId
      }
      .compose { uowId ->
        if (log.isDebugEnabled) log.debug("got uowId $uowId")
        uowIdValue.set(uowId)
        // compute new snapshot
        if (log.isDebugEnabled) log.debug("computing new snapshot")
        val newInstance = uowValue.get().events.fold(snapshotValue.get().state) { state, event -> commandAware.applyEvent.invoke(event, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)
        if (log.isDebugEnabled) log.debug("now will store snapshot $newSnapshot")
        snapshotRepo.upsert(metadata.entityId, newSnapshot)
      }
      .compose({
        // set result
        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
        if (log.isTraceEnabled) log.trace("command handling success: $pair")
        promise.complete(pair)
      }, promise.future())

    return promise.future()
  }
}
