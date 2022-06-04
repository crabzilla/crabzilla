package io.github.crabzilla.stack

import io.github.crabzilla.stack.CrabzillaContext.Companion.toPgConnectionOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber

class DefaultVertxContext(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val pgConfig: JsonObject) : CrabzillaContext {

  override fun vertx(): Vertx = vertx
  override fun pgPool(): PgPool = pgPool

  override fun pgSubscriber(): PgSubscriber {
    return PgSubscriber.subscriber(vertx, toPgConnectionOptions(pgConfig))
  }

}
