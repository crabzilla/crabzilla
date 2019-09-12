package io.github.crabzilla.pgc

import io.github.crabzilla.UnitOfWorkPublisher
import io.github.crabzilla.framework.*
import io.github.crabzilla.internal.CommandController
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PgcCmdHandler<E: Entity>(writeDb: PgPool,
                               private val entityName: String,
                               private val jsonAware: EntityJsonAware<E>,
                               cmdAware: EntityCommandAware<E>,
                               private val uowPublisher: UnitOfWorkPublisher) : EntityComponent<E> {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(PgcCmdHandler::class.java)
  }

  private val uowRepo =  PgcUowRepo(writeDb, jsonAware)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, entityName, cmdAware, jsonAware)
  private val uowJournal = PgcUowJournal(writeDb, jsonAware)
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
          if (log.isTraceEnabled) log.trace("Command successfully handled: $pair. Will publish events.")
          uowPublisher.publish(pair.first, pair.second, Handler { event2 ->
            if (event2.failed()) {
              log.error("When publishing events. This shouldn't never happen.", event2.cause())
              promise.fail(event2.cause())
            } else {
              promise.complete(pair)
            }
          })
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

