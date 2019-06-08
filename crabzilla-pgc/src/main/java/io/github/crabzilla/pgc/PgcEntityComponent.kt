package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.internal.CommandController
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

class PgcEntityComponent<E: Entity>(writeDb: PgPool, entityName: String,
                                    private val jsonAware: EntityJsonAware<E>,
                                    cmdAware: EntityCommandAware<E>,
                                    private val uowPublisher: UnitOfWorkPublisher) : EntityComponent<E> {

  private val uowRepo =  PgcUowRepo(writeDb, jsonAware)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, entityName, cmdAware, jsonAware)
  private val uowJournal = PgcUowJournal(writeDb, jsonAware)
  private val cmdHandler = CommandController(cmdAware, snapshotRepo, uowJournal)

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
    cmdHandler.handle(metadata, command, Handler { event ->
      if (event.succeeded()) {
        val pair = event.result()
        uowPublisher.run {
          publish(pair.first, pair.second, Handler { event2 ->
          if (event2.succeeded()) {
            aHandler.handle(Future.succeededFuture(pair))
          } else {
            aHandler.handle(Future.failedFuture(event2.cause()))
          }
        })
        }
      } else {
        aHandler.handle(Future.failedFuture(event.cause()))
      }
    })
  }

  override fun toJson(state: E): JsonObject {
    return jsonAware.toJson(state)
  }

  override fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command {
    return jsonAware.cmdFromJson(commandName, cmdAsJson)
  }

}

