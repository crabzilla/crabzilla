package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.internal.CommandHandler
import io.github.crabzilla.pgc.PgcCrablet.Companion.PROJECTION_ENDPOINT
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

class PgcEntityComponent<E: Entity>(private val crablet: PgcCrablet, val name: String,
                                    private val jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>)
                                : EntityComponent<E> {

  private val uowRepo =  PgcUowRepo(crablet.writeDb, jsonAware)
  private val snapshotRepo = PgcSnapshotRepo(crablet.writeDb, name, cmdAware, jsonAware)
  private val uowJournal = PgcUowJournal(crablet.writeDb, jsonAware)
  private val cmdHandler = CommandHandler(jsonAware, cmdAware, snapshotRepo, uowJournal)

  override fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>) {
    uowRepo.getUowByUowId(uowId, aHandler)
  }

  override fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>) {
    uowRepo.getAllUowByEntityId(id, aHandler)
  }

  override fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>) {
    snapshotRepo.retrieve(entityId, aHandler)
  }

  override fun handleCommand(metadata: CommandMetadata, json: JsonObject,
                             aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>) {
    cmdHandler.handle(metadata, json, Handler { event ->
      if (event.succeeded()) {
        val pair = event.result()
        crablet.vertx.eventBus().publish(PROJECTION_ENDPOINT, UnitOfWorkEvents.fromUnitOfWork(pair.second, pair.first))
        aHandler.handle(Future.succeededFuture(pair))
      } else {
        aHandler.handle(Future.failedFuture(event.cause()))
      }
    })

  }

  override fun toJson(state: E): JsonObject {
    return jsonAware.toJson(state)
  }
}

