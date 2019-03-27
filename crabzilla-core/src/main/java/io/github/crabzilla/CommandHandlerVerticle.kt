package io.github.crabzilla

import io.github.crabzilla.CommandExecution.RESULT
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.LoggerFactory

class CommandHandlerVerticle<A : Entity>(val name: String,
                                         private val seedValue: A,
                                         private val cmdHandler: (Command, Snapshot<A>) -> CommandResult?,
                                         private val validatorFn: (Command) -> List<String>,
                                         private val applyEventsFn: (DomainEvent, A) -> A,
                                         private val eventJournal: UnitOfWorkRepository,
                                         private val cache: ExpiringMap<Int, Snapshot<A>>)
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

      // command handler function _may_ be blocking if your aggregate is calling blocking services
      vertx.executeBlocking<Snapshot<A>>({ fromCacheFuture ->

        log.info("loading {} from cache", targetId)
        fromCacheFuture.complete(cache[targetId])

      }, false) { fromCacheResult ->

        val snapshotFromCache = if (fromCacheResult.failed()) null else fromCacheResult.result()
        val emptySnapshot = Snapshot(seedValue, 0)
        val cachedSnapshot = snapshotFromCache ?: emptySnapshot

        log.info("id {} cached lastSnapshotData has version {}. Will check if there any version beyond it",
          targetId, cachedSnapshot)

        val selectAfterVersionFuture = Future.future<SnapshotData>()

        eventJournal.selectAfterVersion(targetId, cachedSnapshot.version, selectAfterVersionFuture, name)

        selectAfterVersionFuture.setHandler { event ->

          if (event.failed()) {
            val result = CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR,
              constraints = constraints)
            commandMsg.reply(result)
            return@setHandler
          }

          val nonCached: SnapshotData = event.result()
          val totalOfNonCachedEvents = nonCached.events.size
          log.info("id {} found {} pending events. Last version is now {}", targetId, totalOfNonCachedEvents,
            nonCached.version)

          val resultingSnapshot = if (totalOfNonCachedEvents > 0)
            cachedSnapshot.upgradeTo(nonCached.version, nonCached.events, applyEventsFn)
          else
            cachedSnapshot

          val commandResult: CommandResult? = cmdHandler.invoke(command, resultingSnapshot)

          if (commandResult?.unitOfWork == null) {
            val result = CommandExecution(commandId = command.commandId, result = RESULT.VALIDATION_ERROR,
              constraints = constraints)
            commandMsg.reply(result)
            return@setHandler
          }

          commandResult.inCaseOfSuccess { uow ->

            val appendFuture = Future.future<Int>()
            eventJournal.append(uow!!, appendFuture, name)

            appendFuture.setHandler { appendAsyncResult ->

              if (appendAsyncResult.failed()) {
                val error = appendAsyncResult.cause()
                log.error("appendUnitOfWork for command " + command.commandId, error.message)
                val result = if (error is DbConcurrencyException) {
                  CommandExecution(commandId = command.commandId, result = RESULT.CONCURRENCY_ERROR,
                    constraints = constraints)
                } else {
                  CommandExecution(commandId = command.commandId, result = RESULT.HANDLING_ERROR,
                    constraints = constraints)
                }
                commandMsg.reply(result)
                return@setHandler
              }

              val finalSnapshot = resultingSnapshot.upgradeTo(uow.version, uow.events, applyEventsFn)
              cache[targetId] = finalSnapshot
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

        }

      }

    })

  }

}
