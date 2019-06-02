package io.github.crabzilla.pgc

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.github.crabzilla.*
import io.github.crabzilla.internal.JacksonGenericCodec
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class Crabzilla(val vertx: Vertx, val config: JsonObject, val name: String) {

  init {
    initVertx(vertx)
  }

  val readDb = pgPool("READ", config)
  val writeDb = pgPool("WRITE", config)
  val projectors = mutableListOf<String>()
  val entities = mutableMapOf<String, EntityComponent<out Entity>>()

  companion object {
    var log = LoggerFactory.getLogger(Crabzilla::class.java)
    const val PROJECTION_ENDPOINT = "crabzilla.projection.endpoint"
  }

  fun addProjector(name: String, eventProjector: PgcEventProjector) {
    log.info("adding projector $name")
    val uolProjector = PgcUowProjector(readDb, name)
    projectors.add(name)
    vertx.eventBus().consumer<UnitOfWorkEvents>(PROJECTION_ENDPOINT) { message ->
      uolProjector.handle(message.body(), eventProjector, Handler { result ->
        if (result.failed()) {
          log.error("Projection [$name] failed: " + result.cause().message)
        }
      })
    }
  }

  fun <E: Entity> addEntity(name: String, jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>) {
    log.info("adding entity $name")
    entities[name] = PgcEntityComponent(this, name, jsonAware, cmdAware)
  }

  fun closeDatabases() {
    writeDb.close()
    readDb.close()
  }

  private fun initVertx(vertx: Vertx) {

    Json.mapper
      .registerModule(Jdk8Module())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    vertx.eventBus().registerDefaultCodec(Pair::class.java,
      JacksonGenericCodec(Json.mapper, Pair::class.java))

    vertx.eventBus().registerDefaultCodec(Command::class.java,
      JacksonGenericCodec(Json.mapper, Command::class.java))

    vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
      JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

    vertx.eventBus().registerDefaultCodec(Snapshot::class.java,
      JacksonGenericCodec(Json.mapper, Snapshot::class.java))

    vertx.eventBus().registerDefaultCodec(RangeOfEvents::class.java,
      JacksonGenericCodec(Json.mapper, RangeOfEvents::class.java))

    vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
      JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

    vertx.eventBus().registerDefaultCodec(UnitOfWorkEvents::class.java,
      JacksonGenericCodec(Json.mapper, UnitOfWorkEvents::class.java))

    vertx.eventBus().registerDefaultCodec(CommandMetadata::class.java,
      JacksonGenericCodec(Json.mapper, CommandMetadata::class.java))

  }

  private fun pgPool(id: String, config: JsonObject) : PgPool {
    val writeOptions = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("${id}_DATABASE_HOST"))
      .setDatabase(config.getString("${id}_DATABASE_NAME"))
      .setUser(config.getString("${id}_DATABASE_USER"))
      .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, writeOptions)
  }


}
