package io.github.crabzilla.engine.projector

import io.github.crabzilla.engine.PostgresAbstractVerticle
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * A broker verticle to project events to database
 */
class EventsProjectorVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorVerticle::class.java)
    private const val defaultMetricInterval = 10_000L
  }

  private lateinit var targetEndpoint: String

  private val failures: AtomicLong = AtomicLong(0)
  private val eventSequence: AtomicLong = AtomicLong(0)

  override fun start() {

    targetEndpoint = config().getString("targetEndpoint")

    val serDer = serDer(config())
    val pgPool = pgPool(config())
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    val eventsProjector = provider.create()

    // TODO plug a built in idempotency mechanism here or delegate it to eventsProjector?
    vertx.eventBus().consumer<JsonObject>(targetEndpoint) { msg ->
      val eventRecord = EventRecord.fromJsonObject(msg.body())
      pgPool.withConnection { conn ->
        val event = serDer.eventFromJson(eventRecord.eventAsjJson.toString())
        eventsProjector.project(conn, event, eventRecord.eventMetadata)
      }
        .onFailure {
          msg.fail(500, it.message)
          failures.incrementAndGet()
        }
        .onSuccess {
          msg.reply(true)
          eventSequence.set(eventRecord.eventMetadata.eventSequence)
        }
    }

    vertx.setPeriodic(config().getLong("metricsInterval", defaultMetricInterval)) {
      publishMetrics()
    }

    publishMetrics()
    log.info("Started consuming from endpoint [{}]", targetEndpoint)
  }

  private fun publishMetrics() {
    val metric = JsonObject() // TODO also publish errors
      .put("projectionId", targetEndpoint)
      .put("sequence", eventSequence.get())
    vertx.eventBus().publish("crabzilla.projections", metric)
  }

  override fun stop() {
    log.info("Stopped consuming from endpoint [{}]", targetEndpoint)
  }
}
