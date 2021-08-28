package io.github.crabzilla.engine

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class ObservabilityVerticle(private val interval: Long = 10_000) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(ObservabilityVerticle::class.java)
  }

  private val commandsMetrics = mutableMapOf<String, Pair<Long, Long>>()
  private val commandMetricsInitialized = AtomicBoolean(false)
  private val publicationsMetrics = mutableMapOf<String, Long>()
  private val projectionsMetrics = mutableMapOf<String, Long>()

  override fun start() {

    vertx.eventBus().consumer<JsonObject>("crabzilla.command-controllers") {
      commandsMetrics[it.body().getString("controllerId")] =
        Pair(it.body().getLong("successes"), it.body().getLong("failures"))
      if (!commandMetricsInitialized.get()) {
        action()
        commandMetricsInitialized.set(true)
      }
      it.reply(null)
    }

    vertx.eventBus().consumer<JsonObject>("crabzilla.publications") {
      publicationsMetrics[it.body().getString("publicationId")] =
        it.body().getLong("sequence")
      it.reply(null)
    }

    vertx.eventBus().consumer<JsonObject>("crabzilla.projections") {
      projectionsMetrics[it.body().getString("projectionId")] =
        it.body().getLong("sequence")
      it.reply(null)
    }

    vertx.setPeriodic(interval) {
      action()
    }

    log.info("Started")
  }

  private fun action() {
    val commandControllers = commandsMetrics
      .keys.stream()
      .map {
        val json = JsonObject()
          .put("successes", commandsMetrics[it]!!.first)
          .put("failures", commandsMetrics[it]!!.second)
        JsonObject().put(it, json)
      }.toArray().asList()
    val metric = JsonObject()
      .put("commands-since-startup", JsonArray(commandControllers))
      .put("publications", JsonObject(publicationsMetrics.toMap()))
      .put("projections", JsonObject(projectionsMetrics.toMap()))
    log.info("Stats {}", metric.encodePrettily())
  }

  override fun stop() {
    log.info("Stopped")
  }
}
