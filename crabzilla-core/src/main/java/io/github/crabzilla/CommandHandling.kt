package io.github.crabzilla

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import org.slf4j.LoggerFactory

typealias CommandHandlerFactory<A> = (Command, Snapshot<A>, Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<A>

class CommandHandlerVerticle<A : Entity>(val name: String,
                                         private val seedValue: A,
                                         private val cmdHandlerFactory: CommandHandlerFactory<A>,
                                         private val validatorFn: (Command) -> List<String>,
                                         private val eventJournal: UnitOfWorkRepository,
                                         private val snapshotRepo: SnapshotRepository<A>)
  : AbstractVerticle() {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandHandlerVerticle::class.java)
    internal val options = DeliveryOptions().setCodecName("CommandExecution")
  }

  @Throws(Exception::class)
  override fun start() {

    log.info("starting command verticle for $name")

    vertx.eventBus().consumer<Command>(cmdHandlerEndpoint(name), Handler { commandEvent ->

      val command = commandEvent.body()

      log.info("received a command $command")
      val constraints = validatorFn.invoke(command)

      if (!constraints.isEmpty()) {
        commandEvent.fail(400, constraints.toString())
        return@Handler
      }

      val targetId = command.targetId.value()

      val resultFuture : Future<Pair<UnitOfWork, Int>> = Future.future()

      resultFuture.setHandler { event ->
        if (event.failed()) {
          commandEvent.fail(400, event.cause().message)
        } else {
          commandEvent.reply(event.result())
        }
      }

      val snapshotFuture: Future<Snapshot<A>> = Future.future()
      snapshotRepo.retrieve(targetId, name, snapshotFuture.completer())

      var resultingUow : UnitOfWork? = null

      snapshotFuture

        .compose { snapshot ->
          val commandHandlerFuture = Future.future<UnitOfWork>()
          val cachedSnapshot = snapshot ?: Snapshot(seedValue, 0)
          val cmdHandler = cmdHandlerFactory.invoke(command, cachedSnapshot, commandHandlerFuture.completer())
          cmdHandler.handleCommand()
          commandHandlerFuture
        }

        .compose { unitOfWork ->
          resultingUow = unitOfWork
          val appendFuture = Future.future<Int>()
          eventJournal.append(unitOfWork, name, appendFuture.completer())
          appendFuture
        }

        .compose({ uowSequence ->
          val pair: Pair<UnitOfWork, Int> = Pair<UnitOfWork, Int>(resultingUow!!, uowSequence)
          resultFuture.complete(pair)
        }, resultFuture)

    })

  }

}

abstract class CommandHandler<E: Entity>(val command: Command, val snapshot: Snapshot<E>,
                                         val stateFn: (DomainEvent, E) -> E,
                                         val uowHandler: Handler<AsyncResult<UnitOfWork>>) {

  val uowFuture: Future<UnitOfWork> = Future.future()
  val eventsFuture: Future<List<DomainEvent>> = Future.future()

  init {
    uowFuture.setHandler(uowHandler)
    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(UnitOfWork.of(command, event.result(), snapshot.version + 1))
      } else {
        uowFuture.fail(event.cause())
      }
    }
  }

  abstract fun handleCommand()
}

class DbConcurrencyException(s: String) : RuntimeException(s)
