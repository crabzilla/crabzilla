package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class PgcEntityDeployment<E: Entity>(val name: String,
                                     private val jsonFn: EntityJsonFunctions<E>,
                                     private val stateFn: EntityStateFunctions<E>,
                                     private val cmdFn: EntityCommandFunctions<E>,
                                     writeDb: PgPool) :
  EntityJsonFunctions<E> by jsonFn, EntityStateFunctions<E> by stateFn, EntityCommandFunctions<E> by cmdFn,
  EntityComponent<E> {

  private val uowRepo =  PgcUowRepo(writeDb, jsonFn)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, this)
  private val uowJournal = PgcUowJournal(writeDb, jsonFn)

  val cmdHandlerVerticle = PgcCmdHandlerVerticle(this, snapshotRepo, uowJournal)

  override fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>) {
    uowRepo.getUowByUowId(uowId, aHandler)
  }

  override fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>) {
    uowRepo.getAllUowByEntityId(id, aHandler)
  }

  override fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>) {
    snapshotRepo.retrieve(entityId, aHandler)
  }

}

