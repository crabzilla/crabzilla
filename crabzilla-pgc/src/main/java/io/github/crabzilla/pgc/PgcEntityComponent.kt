package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.Crabzilla.PROJECTION_ENDPOINT
import io.github.crabzilla.internal.CommandHandler
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

class PgcEntityComponent<E: Entity>(val vertx: Vertx, val name: String,
                                    val jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>,
                                    writeDb: PgPool) : EntityComponent<E> {

  private val uowRepo =  PgcUowRepo(writeDb, jsonAware)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, name, cmdAware, jsonAware)
  private val uowJournal = PgcUowJournal(writeDb, jsonAware)
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
        vertx.eventBus().publish(PROJECTION_ENDPOINT, UnitOfWorkEvents.fromUnitOfWork(pair.second, pair.first))
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

