package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.internal.CommandController
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
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

  override fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>) {
    uowRepo.getUowByUowId(uowId, aHandler)
  }

  override fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>) {
    uowRepo.getAllUowByEntityId(id, aHandler)
  }

  override fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>) {
    snapshotRepo.retrieve(entityId, aHandler)
  }

  override fun handleCommand(metadata: CommandMetadata, command: Command,
                             aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>) {

    // TODO optimize this by checking if commandId is null
    uowRepo.getUowByCmdId(metadata.commandId, Handler { gotCommand ->
      if (gotCommand.succeeded()) {
        val uowPair = gotCommand.result()
        if (uowPair != null) {
          aHandler.handle(Future.succeededFuture(uowPair))
          return@Handler
        }
      }
      cmdController.handle(metadata, command, Handler { event ->
        if (event.succeeded()) {
          val pair = event.result()
          log.info("Command successfully handled: $pair. Will publish events.")
          uowPublisher.publish(pair.first, pair.second, Handler { event2 ->
            if (event2.failed()) {
              log.error("When publishing events. This shouldn't never happen.", event2.cause())
              aHandler.handle(Future.failedFuture(event2.cause()))
            } else {
              aHandler.handle(Future.succeededFuture(pair))
            }
          })
        } else {
          log.error("When handling command", event.cause())
          aHandler.handle(Future.failedFuture(event.cause()))
        }
      })
    })

  }

  override fun toJson(state: E): JsonObject {
    return jsonAware.toJson(state)
  }

  override fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command {
    return jsonAware.cmdFromJson(commandName, cmdAsJson)
  }

}

