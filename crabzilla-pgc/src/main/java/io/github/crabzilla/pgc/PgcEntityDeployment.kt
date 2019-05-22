package io.github.crabzilla.pgc

import io.github.crabzilla.Entity
import io.github.crabzilla.EntityCommandFunctions
import io.github.crabzilla.EntityJsonFunctions
import io.github.crabzilla.EntityStateFunctions
import io.reactiverse.pgclient.PgPool

class PgcEntityDeployment<E: Entity>(val name: String,
                                     val jsonFn: EntityJsonFunctions<E>,
                                     private val stateFn: EntityStateFunctions<E>,
                                     private val cmdFn: EntityCommandFunctions<E>,
                                     writeDb: PgPool) :
  EntityJsonFunctions<E> by jsonFn, EntityStateFunctions<E> by stateFn, EntityCommandFunctions<E> by cmdFn {

  val uowRepo = lazy { PgcUowRepo(writeDb, this) }
  val uowJournal = lazy { PgcUowJournal(writeDb, this) }
  val snapshotRepo = lazy { PgcSnapshotRepo(writeDb, this) }
  val cmdHandlerVerticle = lazy { PgcCmdHandlerVerticle(this) }

}
