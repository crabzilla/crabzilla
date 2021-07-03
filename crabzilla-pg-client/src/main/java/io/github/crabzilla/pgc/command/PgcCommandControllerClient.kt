package io.github.crabzilla.pgc.command

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.serialization.json.Json

class PgcCommandControllerClient(val vertx: Vertx, val json: Json, val pgPool: PgPool, val sqlClient: SqlClient) {

  companion object {
    fun create(vertx: Vertx, json: Json, connectOptions: PgConnectOptions, poolOptions: PoolOptions): PgcCommandControllerClient {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return PgcCommandControllerClient(vertx, json, thePgPool, theSqlClient)
    }
  }

  fun close(): Future<Void> {
    return pgPool.close()
      .compose { sqlClient.close() }
  }
}
