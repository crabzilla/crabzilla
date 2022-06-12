package io.github.crabzilla.stack

import io.github.crabzilla.stack.CrabzillaContext.Companion.toPgConnectionOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions

class DefaultVertxContextFactory : CrabzillaContextFactory {

  override fun new(vertx: Vertx, pgConfig: JsonObject): CrabzillaContext {
    fun toPgPool(vertx: Vertx, options: PgConnectOptions): PgPool {
      return PgPool.pool(vertx, options, PoolOptions())
    }
    return DefaultVertxContext(vertx, toPgPool(vertx, toPgConnectionOptions(pgConfig)), pgConfig)
  }

  override fun new(vertx: Vertx, pgConfig: JsonObject, pgPool: PgPool): CrabzillaContext {
    return DefaultVertxContext(vertx, pgPool, pgConfig)
  }

}