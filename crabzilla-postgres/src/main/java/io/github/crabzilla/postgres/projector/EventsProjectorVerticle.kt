package io.github.crabzilla.postgres.projector

import io.github.crabzilla.json.JsonSerDer
import io.github.crabzilla.postgres.EventRecord
import io.github.crabzilla.postgres.PostgresAbstractVerticle
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
    private const val notFound = -1L
  }

  private lateinit var targetEndpoint: String

  private val failures: AtomicLong = AtomicLong(0)
  private val latestOffset: AtomicLong = AtomicLong(0)

  override fun start(promise: Promise<Void>) {
    fun getCurrentOffset(): Future<Long> {
      return pgPool.withConnection { conn ->
        conn.preparedQuery("select sequence from projections where name =$1")
          .execute(Tuple.of(targetEndpoint))
          .map { rowSet ->
            if (rowSet.rowCount() == expectedRows) {
              rowSet.first().getLong("sequence")
            } else {
              notFound
            }
          }
      }
    }
    fun startConsumers(eventsProjector: EventsProjector, serDer: JsonSerDer, pgPool: PgPool) {
      vertx.eventBus().consumer<JsonObject>(targetEndpoint) { msg ->
        val eventRecord = EventRecord.fromJsonObject(msg.body())
        val eventSequence = eventRecord.eventMetadata.eventSequence
        log.info("Event sequence {} current {}", eventSequence, this.latestOffset.get())
        val event = serDer.eventFromJson(eventRecord.eventAsjJson.toString())
        if (eventSequence <= this.latestOffset.get()) {
          log.debug("Ignoring event sequence {} since it's lower than {}", eventSequence, this.latestOffset.get())
          msg.reply(true)
          return@consumer
        }
        pgPool.withTransaction { conn ->
          eventsProjector.project(conn, event, eventRecord.eventMetadata)
            .compose {
              conn
                .preparedQuery("update projections set sequence = $2 where name = $1 and sequence < $2")
                .execute(Tuple.of(targetEndpoint, eventSequence))
            }
        }.onSuccess {
          this.latestOffset.set(eventSequence)
          log.debug("Projected {}", eventSequence)
          msg.reply(true)
        }
          .onFailure {
            failures.incrementAndGet()
            msg.fail(errorCode, it.message)
          }
      }

      vertx.setPeriodic(config().getLong("metricsInterval", defaultMetricInterval)) {
        publishMetrics()
      }
    }

    targetEndpoint = config().getString("targetEndpoint")
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    val eventsProjector = provider.create()

    getCurrentOffset()
      .onFailure {
        promise.fail(it)
      }
      .onSuccess { offset ->
        if (offset == notFound) {
          promise.fail("Projection not found: [$targetEndpoint]")
        } else {
          latestOffset.set(offset)
          startConsumers(eventsProjector, jsonSerDer, pgPool)
          log.info("Started consuming from endpoint [{}]", targetEndpoint)
          promise.complete()
        }
      }
  }

  private fun publishMetrics() {
    val metric = JsonObject()
      .put("projectionId", targetEndpoint)
      .put("failures", failures.get())
      .put("sequence", latestOffset.get())
    vertx.eventBus().publish("crabzilla.projections", metric)
  }

  override fun stop() {
    log.info("Stopped consuming from endpoint [{}]", targetEndpoint)
  }
}
