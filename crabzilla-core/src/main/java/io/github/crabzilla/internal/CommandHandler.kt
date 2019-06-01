package io.github.crabzilla.internal

import io.github.crabzilla.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class CommandHandler<E : Entity>(private val entityJsonFn: EntityJsonAware<E>,
                                 private val entityCmdFn: EntityCommandAware<E>,
                                 private val snapshotRepo: SnapshotRepository<E>,
                                 private val uowJournal: UnitOfWorkJournal<E>) {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandHandler::class.java)
  }

  fun handle(metadata: CommandMetadata, cmdAsJson: JsonObject, aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>) {

    log.trace("received $metadata\n ${cmdAsJson.encodePrettily()}")

    val command: Command? = try {
      entityJsonFn.cmdFromJson(metadata.commandName, cmdAsJson)
    } catch (e: Exception) {
      null
    }

    if (command == null) {
      log.error("Invalid Command json")
      aHandler.handle(Future.failedFuture("Command cannot be deserialized"))
      return
    }

    val constraints = entityCmdFn.validateCmd(command)

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
        log.info("command handler success")
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
        val commandHandlerFuture = Future.future<UnitOfWork>()
        val cachedSnapshot = snapshot ?: Snapshot(entityCmdFn.initialState(), 0)
        val cmdHandler =
          entityCmdFn.cmdHandlerFactory().invoke(metadata, command, cachedSnapshot, commandHandlerFuture)
        cmdHandler.handleCommand()
        snapshotValue.set(cachedSnapshot)
        commandHandlerFuture
      }

      .compose { unitOfWork ->
        log.info("got unitOfWork $unitOfWork")
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
        { state, event -> entityCmdFn.applyEvent(event.second, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)

        log.trace("now will store snapshot $newSnapshot")
        snapshotRepo.upsert(metadata.entityId, newSnapshot, updateSnapshotFuture)
        updateSnapshotFuture
      }

      .compose({
        // set result
        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
        log.info("command handling success: $pair")
        resultFuture.complete(pair)

      }, resultFuture)

  }

}
