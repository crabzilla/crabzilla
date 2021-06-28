package io.github.crabzilla.pgc.api

import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.serialization.json.Json

class PgcClient(val vertx: Vertx, val json: Json, val pgPool: PgPool, val sqlClient: SqlClient) {
  companion object {
    fun create(vertx: Vertx, json: Json, connectOptions: PgConnectOptions, poolOptions: PoolOptions): PgcClient {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return PgcClient(vertx, json, thePgPool, theSqlClient)
    }
  }
}
