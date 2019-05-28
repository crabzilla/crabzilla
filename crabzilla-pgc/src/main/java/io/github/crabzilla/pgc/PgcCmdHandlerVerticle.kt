package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class PgcCmdHandlerVerticle<E : Entity>(private val eDeployment: PgcEntityDeployment<E>,
                                        private val snapshotRepo: PgcSnapshotRepo<E>,
                                        private val uowJournal: PgcUowJournal<E>) : AbstractVerticle() {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcCmdHandlerVerticle::class.java)
  }

  @Throws(Exception::class)
  override fun start() {

    log.info("starting command handler verticle for : ${eDeployment.name}")

    vertx.eventBus().consumer<Pair<CommandMetadata, JsonObject>>(cmdHandlerEndpoint(eDeployment.name), Handler {
      commandEvent ->

      val commandPair = commandEvent.body()

      log.trace("received $commandPair")

      val command: Command? = try { eDeployment.cmdFromJson(commandPair.first.commandName, commandPair.second) }
                              catch (e: Exception) { null }

      if (command == null) {
        log.error("Invalid Command json")
        commandEvent.fail(400, "Command cannot be deserialized")
        return@Handler
      }

      val constraints = eDeployment.validateCmd(command)

      if (constraints.isNotEmpty()) {
        log.error("Command is invalid: $constraints")
        commandEvent.fail(400, constraints.toString())
        return@Handler
      }

      val resultFuture : Future<Pair<UnitOfWork, Long>> = Future.future()

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

      eDeployment.getSnapshot(commandPair.first.entityId, snapshotFuture)

      val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
      val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
      val uowIdValue: AtomicReference<Long> = AtomicReference()

      snapshotFuture

        .compose { snapshot ->
          log.trace("got snapshot $snapshot")
          val commandHandlerFuture = Future.future<UnitOfWork>()
          val cachedSnapshot = snapshot ?: Snapshot(eDeployment.initialState(), 0)
          val cmdHandler =
            eDeployment.cmdHandlerFactory().invoke(commandPair.first, command, cachedSnapshot, commandHandlerFuture)
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
          { state, event -> eDeployment.applyEvent(event.second, state) }
          val newSnapshot = Snapshot(newInstance, uowValue.get().version)

          log.trace("now will store snapshot $newSnapshot")
          snapshotRepo.upsert(commandPair.first.entityId, newSnapshot, updateSnapshotFuture)
          updateSnapshotFuture
        }

        .compose( {
          // set result
          val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
          log.info("command handling success: $pair")
          resultFuture.complete(pair)

        }, resultFuture)

    })

  }

}
