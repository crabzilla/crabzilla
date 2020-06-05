package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.command.AggregateRoot
import io.github.crabzilla.core.command.AggregateRootCommandAware
import io.github.crabzilla.core.command.COMMAND_SERIALIZER
import io.github.crabzilla.core.command.Command
import io.github.crabzilla.core.command.CommandController
import io.github.crabzilla.core.command.CommandMetadata
import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.SnapshotRepository
import io.github.crabzilla.core.command.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AggregateRootWebHandler<A : AggregateRoot>(
        private val crabzillaContext: CrabzillaContext,
        private val snapshotRepo: SnapshotRepository<A>,
        private val cmdAware: AggregateRootCommandAware<A>
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AggregateRootWebHandler::class.java)
  }

  private val cmdController = CommandController(cmdAware, snapshotRepo, crabzillaContext.uowJournal)

  fun entityName(): String {
    return cmdAware.entityName
  }

  fun getUowByUowId(uowId: Long): Future<UnitOfWork?> {
    return crabzillaContext.uowRepository.getUowByUowId(uowId)
  }

  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    return crabzillaContext.uowRepository.selectByAggregateRootId(id)
  }

  fun getSnapshot(entityId: Int): Future<Snapshot<A>> {
    return snapshotRepo.retrieve(entityId)
  }

  fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    crabzillaContext.uowRepository.getUowByCmdId(metadata.commandId).onComplete { gotCommand ->
      if (gotCommand.succeeded()) {
        val uowPair = gotCommand.result()
        if (uowPair != null) {
          promise.complete(uowPair)
          return@onComplete
        }
      }
      cmdController.handle(metadata, command).onComplete { cmdHandled ->
        if (cmdHandled.succeeded()) {
          val pair = cmdHandled.result()
          promise.complete(pair)
          if (log.isDebugEnabled) log.debug("Command successfully handled: $pair")
        } else {
          log.error("When handling command", cmdHandled.cause())
          promise.fail(cmdHandled.cause())
        }
      }
    }
    return promise.future()
  }

  fun toJson(state: A): JsonObject {
    return JsonObject(crabzillaContext.json.stringify(AGGREGATE_ROOT_SERIALIZER, state))
  }

  fun cmdFromJson(cmdAsJson: JsonObject): Command {
    return crabzillaContext.json.parse(COMMAND_SERIALIZER, cmdAsJson.encode())
  }
}
