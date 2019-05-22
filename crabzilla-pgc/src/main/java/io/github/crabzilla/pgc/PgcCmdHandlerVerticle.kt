package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class PgcCmdHandlerVerticle<E : Entity>(private val ed: PgcEntityDeployment<E>) : AbstractVerticle() {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcCmdHandlerVerticle::class.java)
  }

  @Throws(Exception::class)
  override fun start() {

    log.info("starting command handler verticle for : ${ed.name}")

    vertx.eventBus().consumer<Pair<CommandMetadata, JsonObject>>(ed.cmdHandlerEndpoint(), Handler { commandEvent ->

      val commandPair = commandEvent.body()

      log.trace("received $commandPair")

      val command: Command? = try { ed.cmdFromJson(commandPair.first.commandName, commandPair.second) }
                              catch (e: Exception) { null }

      if (command == null) {
        log.error("Invalid Command json")
        commandEvent.fail(400, "Command cannot be deserialized")
        return@Handler
      }

      val constraints = ed.validateCmd(command)

      if (constraints.isNotEmpty()) {
        log.error("Command is invalid: $constraints")
        commandEvent.fail(400, constraints.toString())
        return@Handler
      }

      val resultFuture : Future<Pair<UnitOfWork, Int>> = Future.future()

      resultFuture.setHandler { event ->
        if (event.failed()) {
          log.error("command handler error", event.cause())
          commandEvent.fail(400, event.cause().message)
        } else {
          log.info("command handler success")
          commandEvent.reply(event.result())
        }
      }

      val snapshotFuture: Future<Snapshot<E>> = Future.future()

      ed.snapshotRepo.value.retrieve(commandPair.first.entityId, snapshotFuture)

      val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
      val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
      val uowSeqValue: AtomicReference<Int> = AtomicReference()

      snapshotFuture

        .compose { snapshot ->
          log.trace("got snapshot $snapshot")
          val commandHandlerFuture = Future.future<UnitOfWork>()
          val cachedSnapshot = snapshot ?: Snapshot(ed.initialState(), 0)
          val cmdHandler = ed.cmdHandlerFactory().invoke(commandPair.first, command, cachedSnapshot, commandHandlerFuture)
          cmdHandler.handleCommand()
          snapshotValue.set(cachedSnapshot)
          commandHandlerFuture
        }

        .compose { unitOfWork ->
          log.info("got unitOfWork $unitOfWork")
          val appendFuture = Future.future<Int>()
          // append to journal
          ed.uowJournal.value.append(unitOfWork, appendFuture)
          uowValue.set(unitOfWork)
          appendFuture
        }

        .compose { uowSequence ->
          log.trace("got uowSequence $uowSequence")
          uowSeqValue.set(uowSequence)
          val updateSnapshotFuture = Future.future<Void>()

          // compute new snapshot
          log.trace("computing new snapshot")
          val newInstance = uowValue.get().events.fold(snapshotValue.get().state)
          { state, event -> ed.applyEvent(event.second, state) }
          val newSnapshot = Snapshot(newInstance, uowValue.get().version)

          log.trace("now will store snapshot $newSnapshot")
          ed.snapshotRepo.value.upsert(commandPair.first.entityId, newSnapshot, updateSnapshotFuture)
          updateSnapshotFuture
        }

        .compose( {
          // set result
          val pair: Pair<UnitOfWork, Int> = Pair<UnitOfWork, Int>(uowValue.get(), uowSeqValue.get())
          log.info("command handling success: $pair")
          resultFuture.complete(pair)

        }, resultFuture)

    })

  }

}
