package io.github.crabzilla.vertx

import io.github.crabzilla.*
import io.github.crabzilla.vertx.CommandExecution.RESULT
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.LoggerFactory

class CommandVerticle<A : Entity>(val name: String,
                                  private val seedValue: A,
                                  private val cmdHandler: (Command, Snapshot<A>) -> CommandResult?,
                                  private val validatorFn: (Command) -> List<String>,
                                  private val applyEventsFn: (DomainEvent, A) -> A,
                                  private val eventJournal: UnitOfWorkRepository,
                                  private val cache: ExpiringMap<Int, Snapshot<A>>,
                                  private val circuitBreaker: CircuitBreaker)
  : AbstractVerticle() {

  companion object {
    internal var log = LoggerFactory.getLogger(CommandVerticle::class.java)
  }

  @Throws(Exception::class)
  override fun start() {

    val consumer = vertx.eventBus().consumer<Command>(cmdHandlerEndpoint(name))

    consumer.handler { event ->

      val command = event.body()

      if (command == null) {

        val cmdExecResult = CommandExecution(commandId = command, result = RESULT.VALIDATION_ERROR,
          constraints = listOf("Command cannot be null. Check if JSON payload is valid."))
        event.reply(cmdExecResult)
        return@handler
      }

      log.info("received a command $command")

      val constraints = validatorFn.invoke(command)

      if (!constraints.isEmpty()) {

        val result = CommandExecution(commandId = command.commandId, result = RESULT.VALIDATION_ERROR,
          constraints = constraints)
        event.reply(result)
        return@handler
      }

      circuitBreaker.fallback { throwable ->

        log.error("Fallback for command $command.commandId", throwable)
        val cmdExecResult = CommandExecution(commandId = command.commandId, result = RESULT.FALLBACK)
        event.reply(cmdExecResult)

      }

        .execute<CommandExecution>(cmdHandler(command))

        .setHandler(resultHandler(event))

    }

    log.info("started command verticle for $name")

  }

  private fun cmdHandler(command: Command): (Future<CommandExecution>) -> Unit {

    return { future1 ->

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
            future1.fail(event.cause())
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

          val result: CommandResult? = cmdHandler.invoke(command, resultingSnapshot)

          if (result == null) {
            future1.complete(CommandExecution(result = RESULT.UNKNOWN_COMMAND, commandId = command.commandId))
            return@setHandler
          }

          result.inCaseOfSuccess { uow ->

            val appendFuture = Future.future<Int>()
            eventJournal.append(uow!!, appendFuture, name)

            appendFuture.setHandler { appendAsyncResult ->

              if (appendAsyncResult.failed()) {
                val error = appendAsyncResult.cause()
                log.error("appendUnitOfWork for command " + command.commandId, error.message)
                if (error is DbConcurrencyException) {
                  future1.complete(CommandExecution(result = RESULT.CONCURRENCY_ERROR, commandId = command.commandId,
                    constraints = listOf(error.message ?: "optimistic locking error")))
                } else {
                  future1.fail(appendAsyncResult.cause())
                }
                return@setHandler
              }

              val finalSnapshot = resultingSnapshot.upgradeTo(uow.version, uow.events, applyEventsFn)
              cache[targetId] = finalSnapshot
              val uowSequence = appendAsyncResult.result()
              log.info("uowSequence: {}", uowSequence)
              future1.complete(CommandExecution(result = RESULT.SUCCESS, commandId = command.commandId,
                unitOfWork = uow, uowSequence = uowSequence))
            }
          }

          result.inCaseOfError { error ->
            log.error("commandExecution", error.message)

            future1.complete(CommandExecution(result = RESULT.HANDLING_ERROR, commandId = command.commandId,
              constraints = listOf(error.message ?: "entity handling error")))
          }

        }

      }

    }
  }

  private fun resultHandler(msg: Message<Command>): (AsyncResult<CommandExecution>) -> Unit {

    return { resultHandler: AsyncResult<CommandExecution> ->
      if (!resultHandler.succeeded()) {
        log.error("resultHandler", resultHandler.cause())
        msg.fail(400, resultHandler.cause().message)
      }
      val resp = resultHandler.result()
      val options = DeliveryOptions().setCodecName("CommandExecution")
      msg.reply(resp, options)
    }
  }

}
