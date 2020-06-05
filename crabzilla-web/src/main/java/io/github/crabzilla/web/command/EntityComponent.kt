package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.COMMAND_SERIALIZER
import io.github.crabzilla.core.command.Command
import io.github.crabzilla.core.command.CommandController
import io.github.crabzilla.core.command.CommandMetadata
import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.core.command.ENTITY_SERIALIZER
import io.github.crabzilla.core.command.Entity
import io.github.crabzilla.core.command.EntityCommandAware
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.SnapshotRepository
import io.github.crabzilla.core.command.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EntityComponent<E : Entity>(
  private val ctx: CrabzillaContext,
  private val snapshotRepo: SnapshotRepository<E>,
  private val cmdAware: EntityCommandAware<E>
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(EntityComponent::class.java)
  }

  private val cmdController = CommandController(cmdAware, snapshotRepo, ctx.uowJournal)

  fun entityName(): String {
    return cmdAware.entityName
  }

  fun getUowByUowId(uowId: Long): Future<UnitOfWork?> {
    return ctx.uowRepository.getUowByUowId(uowId)
  }

  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    return ctx.uowRepository.selectByEntityId(id)
  }

  fun getSnapshot(entityId: Int): Future<Snapshot<E>> {
    return snapshotRepo.retrieve(entityId)
  }

  fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    ctx.uowRepository.getUowByCmdId(metadata.commandId).onComplete { gotCommand ->
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

  fun toJson(state: E): JsonObject {
    return JsonObject(ctx.json.stringify(ENTITY_SERIALIZER, state))
  }

  fun cmdFromJson(cmdAsJson: JsonObject): Command {
    return ctx.json.parse(COMMAND_SERIALIZER, cmdAsJson.encode())
  }
}
