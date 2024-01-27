import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.SqlConnection
import java.net.URI
import java.util.*

interface CrabzillaContext {
  companion object {
    const val POSTGRES_NOTIFICATION_CHANNEL = "crabzilla_channel"
    const val EVENTBUS_GLOBAL_TOPIC = "crabzilla.eventbus.global-topic"

    fun toPgConnectionOptions(pgConfig: JsonObject): PgConnectOptions {
      val options = PgConnectOptions()
      val uri = URI.create(pgConfig.getString("url"))
      options.host = uri.host
      options.port = uri.port
      options.database = uri.path.replace("/", "")
      options.user = pgConfig.getString("username")
      options.password = pgConfig.getString("password")
      return options
    }
  }

  fun vertx(): Vertx

  fun pgPool(): PgPool

  fun pgSubscriber(): PgSubscriber

  fun newUUID(): UUID

  fun withinTransaction(commandOperation: (SqlConnection) -> Future<EventMetadata>): Future<EventMetadata>
}

interface CrabzillaContextFactory {
  fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    uuidFunction: () -> UUID = { UUID.randomUUID() },
  ): CrabzillaContext

  fun new(
    vertx: Vertx,
    pgConfig: JsonObject,
    pgPool: PgPool,
    uuidFunction: () -> UUID = { UUID.randomUUID() },
  ): CrabzillaContext
}

/**
 * To project events
 */
interface EventProjector {
  fun project(
    conn: SqlConnection,
    eventRecord: EventRecord,
  ): Future<Void>
}

/**
 * A convention used is the property "type" within JsonObject to figure out what is the type - polymorphism
 */
interface JsonObjectSerDer<T : Any> {
  fun toJson(instance: T): JsonObject

  fun fromJson(json: JsonObject): T
}
