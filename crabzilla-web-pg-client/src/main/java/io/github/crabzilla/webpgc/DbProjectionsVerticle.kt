package io.github.crabzilla.webpgc

import io.github.crabzilla.EventBusChannels
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.readModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import java.lang.management.ManagementFactory
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class DbProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DbProjectionsVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
  }

  private val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }

  override fun start() {
    val implClazz = this::class.java.name
    vertx.eventBus().consumer<String>(implClazz) { msg ->
      if (log.isTraceEnabled) log.trace("$implClazz received " + msg.body())
      msg.reply("$implClazz is already running here: $processId")
    }
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector, json: Json) {
    log.info("adding projector for $projectionName subscribing on ${EventBusChannels.unitOfWorkChannel}")
    vertx.eventBus().consumer<JsonObject>(EventBusChannels.unitOfWorkChannel) { message ->
      val uowEvents = toUnitOfWorkEvents(message.body(), json)
      val uolProjector = PgcUowProjector(readDb, projectionName)
      uolProjector.handle(uowEvents, projector).onComplete { result ->
        if (result.failed()) { // TODO circuit breaker
          log.error("Projection [$projectionName] failed: " + result.cause().message)
        }
      }
    }
  }
}
