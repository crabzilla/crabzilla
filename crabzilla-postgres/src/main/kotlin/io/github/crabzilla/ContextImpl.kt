import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnection
import java.util.*

internal class DefaultCrabzillaContext(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val pgConfig: JsonObject,
  private val uuidFunction: () -> UUID = { UUID.randomUUID() },
) : CrabzillaContext {
  override fun vertx(): Vertx = vertx

  override fun pgPool(): PgPool = pgPool

  override fun pgSubscriber(): PgSubscriber {
    return PgSubscriber.subscriber(vertx, CrabzillaContext.toPgConnectionOptions(pgConfig))
  }

  override fun newUUID(): UUID {
    return uuidFunction.invoke()
  }

  override fun withinTransaction(commandOperation: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata> {
    return pgPool().withTransaction(commandOperation)
  }
}

class DefaultCrabzillaContextFactory : CrabzillaContextFactory {
  override fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    uuidFunction: () -> UUID,
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
      uuidFunction,
    )
  }

  override fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    pgPool: PgPool,
    uuidFunction: () -> UUID,
  ): CrabzillaContext {
    return DefaultCrabzillaContext(vertx, pgPool, pgConfig, uuidFunction)
  }
}
