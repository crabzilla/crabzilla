package io.github.crabzilla

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicReference

data class CommandHandlerEndpoint(val entityName : String) {
  fun endpoint() : String {
    return "$entityName-command-handler"
  }
}

data class CommandMetadata(val entityName: String, val entityId: Int, val commandName: String,
                           val commandId: UUID? = UUID.randomUUID())

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<E>

abstract class CommandHandler<E: Entity>(val cmdMetadata: CommandMetadata, val command: Command,
                                         val snapshot: Snapshot<E>, val stateFn: (DomainEvent, E) -> E,
                                         uowHandler: Handler<AsyncResult<UnitOfWork>>) {
  private val uowFuture: Future<UnitOfWork> = Future.future()
  protected val eventsFuture: Future<List<DomainEvent>> = Future.future()
  init {
    uowFuture.setHandler(uowHandler)
    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(UnitOfWork.of(cmdMetadata.entityId, cmdMetadata.entityName,
          cmdMetadata.commandId?: UUID.randomUUID(),
          cmdMetadata.commandName, command, event.result(), snapshot.version + 1))
      } else {
        uowFuture.fail(event.cause())
      }
    }
  }
  abstract fun handleCommand()
}

class CommandHandlerVerticle<E : Entity>(private val endpoint: CommandHandlerEndpoint,
                                         private val jsonSerDer: EntityJsonSerDer<E>,
                                         private val seedValue: E,
                                         private val applyEventsFn: (DomainEvent, E) -> E,
                                         private val cmdHandlerFactory: CommandHandlerFactory<E>,
                                         private val validatorFn: (Command) -> List<String>,
                                         private val eventJournal: UnitOfWorkJournal,
                                         private val snapshotRepo: SnapshotRepository<E>)
  : AbstractVerticle() {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandHandlerVerticle::class.java)
  }

  @Throws(Exception::class)
  override fun start() {

    log.info("starting command handler verticle for : ${endpoint.entityName}")

    vertx.eventBus().consumer<Pair<CommandMetadata, JsonObject>>(endpoint.endpoint(), Handler { commandEvent ->

      val commandPair = commandEvent.body()

      log.trace("received $commandPair")

      val command: Command? = try { jsonSerDer.cmdFromJson(commandPair.first.commandName, commandPair.second) }
                              catch (e: Exception) { null }

      if (command == null) {
        log.error("Invalid Command json")
        commandEvent.fail(400, "Command cannot be deserialized")
        return@Handler
      }

      val constraints = validatorFn.invoke(command)

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

      snapshotRepo.retrieve(commandPair.first.entityId, snapshotFuture)

      val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
      val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
      val uowSeqValue: AtomicReference<Int> = AtomicReference()

      snapshotFuture

        .compose { snapshot ->
          log.trace("got snapshot $snapshot")
          val commandHandlerFuture = Future.future<UnitOfWork>()
          val cachedSnapshot = snapshot ?: Snapshot(seedValue, 0)
          val cmdHandler = cmdHandlerFactory.invoke(commandPair.first, command, cachedSnapshot, commandHandlerFuture)
          cmdHandler.handleCommand()
          snapshotValue.set(cachedSnapshot)
          commandHandlerFuture
        }

        .compose { unitOfWork ->
          log.info("got unitOfWork $unitOfWork")
          val appendFuture = Future.future<Int>()
          // append to journal
          eventJournal.append(unitOfWork, appendFuture)
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
          { state, event -> applyEventsFn(event, state) }
          val newSnapshot = Snapshot(newInstance, uowValue.get().version)

          log.trace("now will store snapshot $newSnapshot")
          snapshotRepo.upsert(commandPair.first.entityId, newSnapshot, updateSnapshotFuture)
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
