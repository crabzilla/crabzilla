package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

class PgcEntityComponent<E: Entity>(val name: String,
                                    jsonFn: EntityJsonFunctions<E>,
                                    stateFn: EntityStateFunctions<E>,
                                    cmdFn: EntityCommandFunctions<E>,
                                    writeDb: PgPool) : EntityComponent<E> {

  private val uowRepo =  PgcUowRepo(writeDb, jsonFn)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, name, stateFn, jsonFn)
  private val uowJournal = PgcUowJournal(writeDb, jsonFn)

  val cmdHandlerVerticle = PgcCmdHandlerVerticle(name, stateFn, jsonFn, cmdFn, snapshotRepo, uowJournal)

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

