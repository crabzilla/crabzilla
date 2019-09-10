package io.github.crabzilla.internal

import io.github.crabzilla.framework.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class CommandController<E : Entity>(private val commandAware: EntityCommandAware<E>,
                                                                  private val snapshotRepo: SnapshotRepository<E>,
                                                                  private val uowJournal: UnitOfWorkJournal<E>) {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }

  fun handle(metadata: CommandMetadata, command: Command, aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>) {

    log.trace("received $metadata\n $command")

    val constraints = commandAware.validateCmd(command)

    if (constraints.isNotEmpty()) {
      log.error("Command is invalid: $constraints")
      aHandler.handle(Future.failedFuture(constraints.toString()))
      return
    }

    val resultFuture: Future<Pair<UnitOfWork, Long>> = Future.future()

    resultFuture.setHandler { event ->
      if (event.failed()) {
        log.error("command handler error", event.cause())
        aHandler.handle(Future.failedFuture(event.cause().message))
        return@setHandler
      } else {
        log.trace("command handler success")
        aHandler.handle(Future.succeededFuture(event.result()))
      }
    }

    val snapshotFuture: Future<Snapshot<E>> = Future.future()

    snapshotRepo.retrieve(metadata.entityId, snapshotFuture)

    val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
    val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
    val uowIdValue: AtomicReference<Long> = AtomicReference()

    snapshotFuture

      .compose { snapshot ->
        log.trace("got snapshot $snapshot")
        val cmdHandlerFuture = Future.future<UnitOfWork>()
        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState(), 0)
        val cmdHandler = commandAware.cmdHandlerFactory()
          .createHandler(metadata, command, cachedSnapshot, cmdHandlerFuture)
        cmdHandler.handleCommand()
        snapshotValue.set(cachedSnapshot)
        cmdHandlerFuture
      }

      .compose { unitOfWork ->
        log.trace("got unitOfWork $unitOfWork")
        val appendFuture = Future.future<Long>()
        // append to journal
        uowJournal.append(unitOfWork, appendFuture)
        uowValue.set(unitOfWork)
        appendFuture
      }

      .compose { uowId ->
        log.trace("got uowId $uowId")
        uowIdValue.set(uowId)
        val updateSnapshotFuture = Future.future<Void>()

        // compute new snapshot
        log.trace("computing new snapshot")
        val newInstance = uowValue.get().events.fold(snapshotValue.get().state)
        { state, event -> commandAware.applyEvent(event.second, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)

        log.trace("now will store snapshot $newSnapshot")
        snapshotRepo.upsert(metadata.entityId, newSnapshot, updateSnapshotFuture)
        updateSnapshotFuture
      }

      .compose({
        // set result
        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
        log.trace("command handling success: $pair")
        resultFuture.complete(pair)

      }, resultFuture)

  }

}
