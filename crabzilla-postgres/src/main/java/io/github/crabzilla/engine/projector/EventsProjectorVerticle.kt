package io.github.crabzilla.engine.projector

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.engine.EventRecord
import io.github.crabzilla.engine.PostgresAbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * A broker verticle to project events to database
 */
class EventsProjectorVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorVerticle::class.java)
    private const val defaultMetricInterval = 10_000L
    private const val errorCode = 500
    private const val expectedRows = 1
  }

  private lateinit var targetEndpoint: String

  private val failures: AtomicLong = AtomicLong(0)
  private val eventSequence: AtomicLong = AtomicLong(0)

  override fun start(promise: Promise<Void>) {

    fun consumers(eventsProjector: EventsProjector, serDer: JsonSerDer, pgPool: PgPool) {
      vertx.eventBus().consumer<JsonObject>(targetEndpoint) { msg ->
        val eventRecord = EventRecord.fromJsonObject(msg.body())
        log.info(
          "Event sequence {} current {}",
          eventRecord.eventMetadata.eventSequence, eventSequence.get()
        )
        val event = serDer.eventFromJson(eventRecord.eventAsjJson.toString())
        if (eventRecord.eventMetadata.eventSequence <= eventSequence.get()) {
          log.debug(
            "Ignoring event sequence {} since it's lower than {}",
            eventRecord.eventMetadata.eventSequence, eventSequence.get()
          )
          msg.reply(true)
          return@consumer
        }
        pgPool.withTransaction { conn ->
          eventsProjector.project(conn, event, eventRecord.eventMetadata)
            .compose { projectedSequence ->
              log.debug("Projected {}", projectedSequence)
              conn
                .preparedQuery("update projections set sequence = $2 where name = $1 and sequence < $2")
                .execute(Tuple.of(targetEndpoint, projectedSequence))
            }
        }
          .onFailure {
            msg.fail(errorCode, it.message)
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
    }

    targetEndpoint = config().getString("targetEndpoint")

    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    val eventsProjector = provider.create()

    pgPool.withConnection { conn ->
      conn.preparedQuery("select sequence from projections where name =$1")
        .execute(Tuple.of(targetEndpoint))
        .map { row ->
          if (row.rowCount() == expectedRows) {
            eventSequence.set(row.first().getLong("sequence"))
            Future.succeededFuture<Void>()
          } else {
            Future.failedFuture("Projection not found: [$targetEndpoint]")
          }
        }
    }
      .onFailure {
        promise.fail(it)
      }
      .onSuccess {
        consumers(eventsProjector, jsonSerDer, pgPool)
        log.info("Started consuming from endpoint [{}]", targetEndpoint)
        promise.complete()
      }
  }

  private fun publishMetrics() {
    val metric = JsonObject()
      .put("projectionId", targetEndpoint)
      .put("failures", failures.get())
      .put("sequence", eventSequence.get())
    vertx.eventBus().publish("crabzilla.projections", metric)
  }

  override fun stop() {
    log.info("Stopped consuming from endpoint [{}]", targetEndpoint)
  }
}
