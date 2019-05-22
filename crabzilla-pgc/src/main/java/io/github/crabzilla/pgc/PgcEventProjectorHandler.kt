package io.github.crabzilla.pgc

import io.github.crabzilla.DomainEvent
import io.reactiverse.pgclient.PgTransaction
import io.vertx.core.Future

interface PgcEventProjectorHandler {

  fun handle(pgConn: PgTransaction, targetId: Int, event: DomainEvent) : Future<Void>

}
