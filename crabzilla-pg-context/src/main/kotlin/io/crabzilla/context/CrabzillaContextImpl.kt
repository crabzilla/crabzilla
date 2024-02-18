package io.crabzilla.context

import io.crabzilla.context.CrabzillaContext.Companion.toPgConnectionOptions
import io.crabzilla.context.CrabzillaContext.Companion.toPgPool
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Pool
import java.util.*

class CrabzillaContextImpl(
  override val vertx: Vertx,
  private val pgConfig: JsonObject,
  override val uuidFunction: () -> UUID = { UUID.randomUUID() },
) : CrabzillaContext {
  override val pgPool: Pool by lazy {
    toPgPool(vertx, toPgConnectionOptions(pgConfig))
  }

  override fun newPgSubscriber(): PgSubscriber {
    return PgSubscriber.subscriber(vertx, toPgConnectionOptions(pgConfig))
  }
}
