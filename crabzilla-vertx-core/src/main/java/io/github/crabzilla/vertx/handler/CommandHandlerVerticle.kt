package io.github.crabzilla.vertx.handler

import io.github.crabzilla.core.*
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.DbConcurrencyException
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.VerticleRole.HANDLER
import io.github.crabzilla.vertx.handler.CommandExecution.RESULT.*
import io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory.getLogger
import net.jodah.expiringmap.ExpiringMap

class CommandHandlerVerticle<A : Entity>(override val name: String,
                                         private val seedValue: A,
                                         private val cmdHandler: (EntityCommand, Snapshot<A>) -> CommandResult?,
                                         private val validatorFn: (EntityCommand) -> List<String>,
                                         private val snapshotPromoter: SnapshotPromoter<A>,
                                         private val eventJournal: UnitOfWorkRepository,
                                         private val cache: ExpiringMap<String, Snapshot<A>>,
                                         private val circuitBreaker: CircuitBreaker) : CrabzillaVerticle(name, HANDLER) {

  @Throws(Exception::class)
  override fun start() {

    val consumer = vertx.eventBus().consumer<EntityCommand>(cmdHandlerEndpoint(name))

    consumer.handler({ msg ->

      val command = msg.body()

      if (command == null) {

        val cmdExecResult = CommandExecution(commandId = command, result = VALIDATION_ERROR,
          constraints = listOf("Command cannot be null. Check if JSON payload is valid."))
        msg.reply(cmdExecResult)
        return@handler
      }

      log.info("received a command $command")

      val constraints = validatorFn.invoke(command)

      if (!constraints.isEmpty()) {

        val result = CommandExecution(commandId = command.commandId, result = VALIDATION_ERROR,
          constraints = constraints)
        msg.reply(result)
        return@handler
      }

      circuitBreaker.fallback { throwable ->

        log.error("Fallback for command $command.commandId", throwable)
        val cmdExecResult = CommandExecution(commandId = command.commandId, result = FALLBACK)
        msg.reply(cmdExecResult)

      }

        .execute<CommandExecution>(cmdHandler(command))

        .setHandler(resultHandler(msg))

    })

  }

  internal fun cmdHandler(command: EntityCommand): (Future<CommandExecution>) -> Unit {

    return { future1 ->

      val targetId = command.targetId.stringValue()

      // get from cache _may_ be blocking if you plug an EntryLoader (from ExpiringMap)
      vertx.executeBlocking<Snapshot<A>>({ fromCacheFuture ->

        log.debug("loading {} from cache", targetId)

        fromCacheFuture.complete(cache[targetId])

      }, false) { fromCacheResult ->

        if (fromCacheResult.failed()) {
          future1.fail(fromCacheResult.cause())
          return@executeBlocking
        }

        val snapshotFromCache = fromCacheResult.result()
        val emptySnapshot = Snapshot(seedValue, 0)
        val cachedSnapshot = snapshotFromCache ?: emptySnapshot

        log.debug("id {} cached lastSnapshotData has version {}. Will check if there any version beyond it",
                targetId, cachedSnapshot)

        val selectAfterVersionFuture = Future.future<SnapshotData>()

        // command handler function _may_ be blocking if your aggregate are using blocking internal services
        eventJournal.selectAfterVersion(targetId, cachedSnapshot.version, selectAfterVersionFuture, name)

        selectAfterVersionFuture.setHandler { fromEventRepoResult ->

          if (fromEventRepoResult.failed()) {
            future1.fail(fromEventRepoResult.cause())
            return@setHandler
          }

          val nonCached = fromEventRepoResult.result()
          val totalOfNonCachedEvents = nonCached.events.size
          log.debug("id {} found {} pending events. Last version is now {}", targetId, totalOfNonCachedEvents,
                  nonCached.version)

          // cmd handler _may_ be blocking. Otherwise, aggregate root would need to use reactive API to call
          // external services
          vertx.executeBlocking<CommandExecution>({ future2 ->

            val resultingSnapshot = if (totalOfNonCachedEvents > 0)
              snapshotPromoter.promote(cachedSnapshot, nonCached.version, nonCached.events)
            else
              cachedSnapshot

            if (totalOfNonCachedEvents > 0) {
              cache.put(targetId, resultingSnapshot)
            }

            val result = cmdHandler.invoke(command, resultingSnapshot)

            if (result == null) {
              future2.complete(CommandExecution(result = UNKNOWN_COMMAND, commandId = command.commandId))
              return@executeBlocking
            }

            result.inCaseOfSuccess({ uow ->

              val appendFuture = Future.future<Long>()
              eventJournal.append(uow!!, appendFuture, name)

              appendFuture.setHandler { appendAsyncResult ->

                if (appendAsyncResult.failed()) {
                  val error = appendAsyncResult.cause()
                  log.error("appendUnitOfWork for command " + command.commandId, error.message)
                  if (error is DbConcurrencyException) {
                    future2.complete(CommandExecution(result = CONCURRENCY_ERROR, commandId = command.commandId,
                      constraints = listOf(error.message)))
                  } else {
                    future2.fail(appendAsyncResult.cause())
                  }
                  return@setHandler
                }

                val finalSnapshot = snapshotPromoter.promote(resultingSnapshot, uow.version, uow.events)
                cache.put(targetId, finalSnapshot)
                val uowSequence = appendAsyncResult.result()
                log.debug("uowSequence: {}", uowSequence)
                future2.complete(CommandExecution(result = SUCCESS, commandId = command.commandId,
                  unitOfWork = uow, uowSequence = uowSequence))
              }
            })

            result.inCaseOfError({ error ->
              log.error("commandExecution", error.message)
                future2.complete(CommandExecution(result = HANDLING_ERROR, commandId = command.commandId,
                  constraints = listOf(error.message)))
            })

          }) { res ->
            if (res.succeeded()) {
              future1.complete(res.result())
            } else {
              log.error("selectAfterVersion ", res.cause())
              future1.fail(res.cause())
            }
          }
        }

      }

    }
  }

  internal fun resultHandler(msg: Message<EntityCommand>): (AsyncResult<CommandExecution>) -> Unit {

    return { resultHandler: AsyncResult<CommandExecution> ->
      if (!resultHandler.succeeded()) {
        log.error("resultHandler", resultHandler.cause())
        // TODO customize
        msg.fail(400, resultHandler.cause().message)
      }
      val resp = resultHandler.result()
      val options = DeliveryOptions().setCodecName("CommandExecution")
      msg.reply(resp, options)
      //      msg.reply(resp, options, (Handler<AsyncResult<Message<CommandExecution>>>)
      //              event -> {
      //        if (!event.succeeded()) {
      //          log.error("msg.reply ", event.cause());
      //        }
      //      });
    }
  }

  companion object {

    internal var log = getLogger(CommandHandlerVerticle::class.java)
  }

}

