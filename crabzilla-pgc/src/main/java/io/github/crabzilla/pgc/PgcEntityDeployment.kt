package io.github.crabzilla.pgc

import io.github.crabzilla.Entity
import io.github.crabzilla.EntityCommandFunctions
import io.github.crabzilla.EntityJsonFunctions
import io.github.crabzilla.EntityStateFunctions
import io.reactiverse.pgclient.PgPool

class PgcEntityDeployment<E: Entity>(val name: String,
                                     val ejson: EntityJsonFunctions<E>,
                                     val eState: EntityStateFunctions<E>,
                                     val eCmd: EntityCommandFunctions<E>,
                                     writeDb: PgPool) :
  EntityJsonFunctions<E> by ejson, EntityStateFunctions<E> by eState, EntityCommandFunctions<E> by eCmd {

  val uowRepo = PgcUowRepo(writeDb, this)
  val uowJournal = PgcUowJournal(writeDb, this)
  val snapshotRepo = PgcSnapshotRepo(writeDb, this)
  val cmdHandlerVerticle = PgcCmdHandlerVerticle(this)

  fun cmdHandlerEndpoint(): String {
    return "$name-cmd-handler"
  }

}
