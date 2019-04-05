package io.github.crabzilla

import io.github.crabzilla.CommandExecution.RESULT
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import org.slf4j.LoggerFactory

class CommandHandlerVerticle<A : Entity>(val name: String,
                                         private val seedValue: A,
                                         private val cmdHandler: (Command, Snapshot<A>) -> CommandResult?,
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

    vertx.eventBus().consumer<Command>(cmdHandlerEndpoint(name), Handler { commandMsg ->

      val command = commandMsg.body()

      log.info("received a command $command")
      val constraints = validatorFn.invoke(command)

      if (!constraints.isEmpty()) {
        val result = CommandExecution(commandId = command.commandId, result = RESULT.VALIDATION_ERROR,
          constraints = constraints)
        commandMsg.reply(result)
        return@Handler
      }

      val targetId = command.targetId.value()

      snapshotRepo.retrieve(targetId, name, Handler { fromCacheResult ->

        if (fromCacheResult.failed()) {
          val result = CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR)
          commandMsg.reply(result)
          return@Handler
        }

        val snapshotFromCache = if (fromCacheResult.failed()) null else fromCacheResult.result()
        val cachedSnapshot = snapshotFromCache ?: Snapshot(seedValue, 0)

        val commandResult: CommandResult? = cmdHandler.invoke(command, cachedSnapshot)

        if (commandResult?.unitOfWork == null) {
          val result = CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR)
          commandMsg.reply(result)
          return@Handler
        }

        commandResult.inCaseOfSuccess { uow ->

          val appendFuture = Future.future<Int>()

          eventJournal.append(uow!!, appendFuture, name)

          appendFuture.setHandler { appendAsyncResult ->

            if (appendAsyncResult.failed()) {
              val error = appendAsyncResult.cause()
              log.error("appendUnitOfWork for command " + command.commandId, error.message)
              val result = if (error is DbConcurrencyException) {
                CommandExecution(commandId = command.commandId, result = RESULT.CONCURRENCY_ERROR)
              } else {
                CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR)
              }
              commandMsg.reply(result)
              return@setHandler
            }

            val uowSequence = appendAsyncResult.result()
            log.info("uowSequence: {}", uowSequence)
            commandMsg.reply(CommandExecution(result = RESULT.SUCCESS, commandId = command.commandId,
              unitOfWork = uow, uowSequence = uowSequence), options)
          }

        }

        commandResult.inCaseOfError { error ->
          log.info("error: {}", error)
          commandMsg.reply(CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR))
          return@inCaseOfError
        }

      })

    })

  }

  class DbConcurrencyException(s: String) : RuntimeException(s)

}
