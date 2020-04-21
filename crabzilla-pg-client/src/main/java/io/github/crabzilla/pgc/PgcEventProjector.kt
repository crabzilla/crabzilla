package io.github.crabzilla.pgc

import io.github.crabzilla.framework.DomainEvent
import io.vertx.core.Future
import io.vertx.sqlclient.Transaction

interface PgcEventProjector {
  fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void>
}
