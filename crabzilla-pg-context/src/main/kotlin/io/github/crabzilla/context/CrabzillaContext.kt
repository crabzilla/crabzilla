package io.github.crabzilla.context

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnection
import java.net.URI
import java.util.*

/**
 * A context for running Crabzilla
 */
interface CrabzillaContext {
  val uuidFunction: () -> UUID

  val vertx: Vertx

  val pgPool: Pool

  fun newPgSubscriber(): PgSubscriber

  fun withinTransaction(commandOperation: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata>

  companion object {
    const val POSTGRES_NOTIFICATION_CHANNEL = "crabzilla_channel"

    fun toPgConnectionOptions(pgConfig: JsonObject): PgConnectOptions {
      val options = PgConnectOptions()
      val uri = URI.create(pgConfig.getString("uri"))
      options.host = uri.host
      options.port = uri.port
      options.database = uri.path.replace("/", "")
      options.user = pgConfig.getString("username")
      options.password = pgConfig.getString("password")
      return options
    }

    fun toPgPool(
      vertx: Vertx,
      options: PgConnectOptions,
      pgPoolOptions: PoolOptions? = DEFAULT_POOL_OPTIONS,
    ): Pool {
      return PgBuilder
        .pool()
        .connectingTo(options)
        .using(vertx)
        .with(pgPoolOptions)
        .build()
    }

    private val DEFAULT_POOL_OPTIONS: PoolOptions =
      PoolOptions()
        .setMaxSize(10)
        .setShared(true)
        .setName("crabzilla-shared-postgres-pool")
    //  .setEventLoopSize(4)
  }
}

/**
 * A convention used is the property "type" within JsonObject to figure out what is the type - polymorphism
 */
interface JsonObjectSerDer<T : Any> {
  fun toJson(instance: T): JsonObject

  fun fromJson(json: JsonObject): T
}

/**
 * To project events within a transaction
 */
interface EventProjector {
  fun project(
    conn: SqlConnection,
    eventRecord: EventRecord,
  ): Future<Void>
}

sealed class CrabzillaWriterException(override val message: String, override val cause: Throwable? = null) :
  RuntimeException(message, cause) {
  class StreamMustBeNewException(message: String) : CrabzillaWriterException(message)

  class StreamCantBeLockedException(message: String) : CrabzillaWriterException(message)

  class BusinessException(message: String, cause: Throwable) : CrabzillaWriterException(message, cause)
}
