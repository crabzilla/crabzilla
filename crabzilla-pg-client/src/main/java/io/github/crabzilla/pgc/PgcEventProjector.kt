package io.github.crabzilla.pgc

import io.github.crabzilla.framework.DomainEvent
import io.vertx.core.Promise
import io.vertx.sqlclient.Transaction

interface PgcEventProjector {

  fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent) : Promise<Void>

  fun ok(): Promise<Void> {
    val promise = Promise.promise<Void>()
    promise.complete()
    return promise
  }

}
