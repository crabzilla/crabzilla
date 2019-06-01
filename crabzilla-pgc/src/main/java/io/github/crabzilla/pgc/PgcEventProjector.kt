package io.github.crabzilla.pgc

import io.github.crabzilla.DomainEvent
import io.reactiverse.pgclient.PgTransaction
import io.vertx.core.Future

interface PgcEventProjector {

  fun handle(pgTx: PgTransaction, targetId: Int, event: DomainEvent) : Future<Void>

}
