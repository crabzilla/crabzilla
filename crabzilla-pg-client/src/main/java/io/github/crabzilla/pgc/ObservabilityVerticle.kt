package io.github.crabzilla.pgc

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class ObservabilityVerticle(private val interval: Long = 10_000) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(ObservabilityVerticle::class.java)
  }

  private val commandsMetrics = mutableMapOf<String, Long>()
  private val publicationsMetrics = mutableMapOf<String, Long>()
  private val projectionsMetrics = mutableMapOf<String, Long>()

  override fun start() {

    vertx.eventBus().consumer<JsonObject>("crabzilla.command-controllers") {
      commandsMetrics[it.body().getString("controllerId")] = it.body().getLong("successes")
      it.reply(null)
    }

    vertx.eventBus().consumer<JsonObject>("crabzilla.publications") {
      publicationsMetrics[it.body().getString("publicationId")] = it.body().getLong("sequence")
      it.reply(null)
    }

    vertx.eventBus().consumer<JsonObject>("crabzilla.projections") {
      projectionsMetrics[it.body().getString("projectionId")] = it.body().getLong("sequence")
      it.reply(null)
    }

    vertx.setPeriodic(interval) {
      action()
    }

    log.info("Started")
    action()
  }

  private fun action() {
    val metric = JsonObject()
      .put("commands-since-startup", JsonObject(commandsMetrics.toMap()))
      .put("publications", JsonObject(publicationsMetrics.toMap()))
      .put("projections", JsonObject(projectionsMetrics.toMap()))
    log.info("Stats {}", metric.encodePrettily())
  }

  override fun stop() {
    log.info("Stopped")
  }
}
