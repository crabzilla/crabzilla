package io.github.crabzilla.pgc

import io.github.crabzilla.framework.*
import io.github.crabzilla.internal.CommandController
import io.github.crabzilla.internal.EntityComponent
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PgcCmdHandler<E: Entity>(vertx: Vertx,
                               writeDb: PgPool,
                               private val entityName: String,
                               private val jsonAware: EntityJsonAware<E>,
                               cmdAware: EntityCommandAware<E>) : EntityComponent<E> {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(PgcCmdHandler::class.java)
  }

  private val uowRepo =  PgcUowRepo(writeDb, jsonAware)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, entityName, cmdAware, jsonAware)
  private val uowJournal = PgcUowJournal(vertx, writeDb, jsonAware)
  private val cmdController = CommandController(cmdAware, snapshotRepo, uowJournal)

  override fun entityName(): String {
    return entityName
  }

  override fun getUowByUowId(uowId: Long) : Promise<UnitOfWork> {
    return uowRepo.getUowByUowId(uowId)
  }

  override fun getAllUowByEntityId(id: Int): Promise<List<UnitOfWork>> {
    return uowRepo.getAllUowByEntityId(id)
  }

  override fun getSnapshot(entityId: Int): Promise<Snapshot<E>> {
    return snapshotRepo.retrieve(entityId)
  }

  override fun handleCommand(metadata: CommandMetadata, command: Command): Promise<Pair<UnitOfWork, Long>> {

    val promise = Promise.promise<Pair<UnitOfWork, Long>>()

    uowRepo.getUowByCmdId(metadata.commandId).future().setHandler { gotCommand ->
      if (gotCommand.succeeded()) {
        val uowPair = gotCommand.result()
        if (uowPair != null) {
          promise.complete(uowPair)
          return@setHandler
        }
      }
      cmdController.handle(metadata, command).future().setHandler { cmdHandled ->
        if (cmdHandled.succeeded()) {
          val pair = cmdHandled.result()
          promise.complete(pair)
          if (log.isTraceEnabled) log.trace("Command successfully handled: $pair")
        } else {
          log.error("When handling command", cmdHandled.cause())
          promise.fail(cmdHandled.cause())
        }
      }
    }

    return promise
  }

  override fun toJson(state: E): JsonObject {
    return jsonAware.toJson(state)
  }

  override fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command {
    return jsonAware.cmdFromJson(commandName, cmdAsJson)
  }

}

