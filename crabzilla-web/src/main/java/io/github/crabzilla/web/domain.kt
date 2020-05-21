package io.github.crabzilla.web

import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.CrabzillaContext
import io.github.crabzilla.core.CrabzillaInternal.CommandController
import io.github.crabzilla.core.ENTITY_SERIALIZER
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityCommandAware
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.SnapshotRepository
import io.github.crabzilla.core.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("addResourceForEntity")

class WebResourceContext<E : Entity>(
  val cmdTypeMap: Map<String, String>,
  val cmdAware: EntityCommandAware<E>,
  val snapshotRepo: SnapshotRepository<E>
)

fun <E : Entity> addResourceForEntity(router: Router, ctx: CrabzillaContext, webCtx: WebResourceContext<E>) {
  log.info("adding web command handler for entity ${webCtx.cmdAware.entityName}")
  val cmdHandlerComponent = EntityComponent(ctx, webCtx.snapshotRepo, webCtx.cmdAware)
  WebDeployer(webCtx.cmdAware.entityName, webCtx.cmdTypeMap, cmdHandlerComponent, router).deployWebRoutes()
}

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

  fun getUowByUowId(uowId: Long): Future<UnitOfWork> {
    return ctx.uowRepository.getUowByUowId(uowId)
  }

  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    return ctx.uowRepository.getAllUowByEntityId(id)
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
