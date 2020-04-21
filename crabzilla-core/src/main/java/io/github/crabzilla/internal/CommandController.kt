package io.github.crabzilla.internal

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.CommandMetadata
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityCommandAware
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Promise
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory

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
    if (log.isTraceEnabled) log.trace("received $metadata\n $command")
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
        if (log.isTraceEnabled) log.trace("got snapshot $snapshot")
        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
        snapshotValue.set(cachedSnapshot)
        val cmdHandler = commandAware.cmdHandlerFactory.invoke(metadata, command, cachedSnapshot)
        cmdHandler.handleCommand()
      }
      .compose { unitOfWork ->
        if (log.isTraceEnabled) log.trace("got unitOfWork $unitOfWork")
        // append to journal
        uowValue.set(unitOfWork)
        uowJournal.append(unitOfWork)
      }
      .compose { uowId ->
        if (log.isTraceEnabled) log.trace("got uowId $uowId")
        uowIdValue.set(uowId)
        // compute new snapshot
        if (log.isTraceEnabled) log.trace("computing new snapshot")
        val newInstance = uowValue.get().events.fold(snapshotValue.get().state) { state, event -> commandAware.applyEvent.invoke(event, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)
        if (log.isTraceEnabled) log.trace("now will store snapshot $newSnapshot")
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
