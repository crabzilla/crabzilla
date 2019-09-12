package io.github.crabzilla.internal

import io.github.crabzilla.framework.*
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class CommandController<E : Entity>(private val commandAware: EntityCommandAware<E>,
                                    private val snapshotRepo: SnapshotRepository<E>,
                                    private val uowJournal: UnitOfWorkJournal<E>) {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: Command) : Promise<Pair<UnitOfWork, Long>> {

    val promise = Promise.promise<Pair<UnitOfWork, Long>>()

    log.info("received $metadata\n $command")

    val constraints = commandAware.validateCmd(command)

    if (constraints.isNotEmpty()) {
      log.error("Command is invalid: $constraints")
      promise.fail(constraints.toString())
      return promise
    }

    val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
    val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
    val uowIdValue: AtomicReference<Long> = AtomicReference()

    snapshotRepo.retrieve(metadata.entityId).future()

      .compose { snapshot ->
        log.info("got snapshot $snapshot")
        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
        snapshotValue.set(cachedSnapshot)
        val cmdHandler = commandAware.cmdHandlerFactory.invoke(metadata, command, cachedSnapshot)
        cmdHandler.handleCommand().future()
      }

      .compose { unitOfWork ->
        log.info("got unitOfWork $unitOfWork")
        // append to journal
        uowValue.set(unitOfWork)
        uowJournal.append(unitOfWork).future()
      }

      .compose { uowId ->
        log.info("got uowId $uowId")
        uowIdValue.set(uowId)
        // compute new snapshot
        log.info("computing new snapshot")
        val newInstance = uowValue.get().events.fold(snapshotValue.get().state)
        { state, event -> commandAware.applyEvent.invoke(event.second, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)
        log.info("now will store snapshot $newSnapshot")
        snapshotRepo.upsert(metadata.entityId, newSnapshot).future()
      }

      .compose({
        // set result
        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
        log.info("command handling success: $pair")
        promise.complete(pair)

      }, promise.future())

    return promise

  }

}
