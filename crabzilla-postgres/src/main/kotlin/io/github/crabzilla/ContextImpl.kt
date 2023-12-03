import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions

internal class DefaultCrabzillaContext(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val pgConfig: JsonObject,
  private val ulidFunction: () -> String,
) : CrabzillaContext {
  override fun vertx(): Vertx = vertx

  override fun pgPool(): PgPool = pgPool

  override fun pgSubscriber(): PgSubscriber {
    return PgSubscriber.subscriber(vertx, CrabzillaContext.toPgConnectionOptions(pgConfig))
  }

  override fun nextUlid(): String {
    return ulidFunction.invoke()
  }
}

class DefaultCrabzillaContextFactory : CrabzillaContextFactory {
  override fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    ulidFunction: () -> String,
  ): CrabzillaContext {
    fun toPgPool(
      vertx: Vertx,
      options: PgConnectOptions,
    ): PgPool {
      return PgPool.pool(vertx, options, PoolOptions())
    }
    return DefaultCrabzillaContext(
      vertx,
      toPgPool(vertx, CrabzillaContext.toPgConnectionOptions(pgConfig)),
      pgConfig,
      ulidFunction,
    )
  }

  override fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    pgPool: PgPool,
    ulidFunction: () -> String,
  ): CrabzillaContext {
    return DefaultCrabzillaContext(vertx, pgPool, pgConfig, ulidFunction)
  }
}
