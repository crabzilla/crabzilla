package io.github.crabzilla.pgc.api

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.PgcEventsProjector
import io.github.crabzilla.pgc.PgcEventsProjectorVerticle
import io.github.crabzilla.pgc.PgcEventsScanner
import io.github.crabzilla.stack.EventBusPublisher
import io.github.crabzilla.stack.EventsPublisherOptions
import io.github.crabzilla.stack.EventsPublisherVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Verticle
import org.slf4j.LoggerFactory
import java.util.function.BiFunction

class PgcVerticlesClient(private val pgcClient: PgcClient) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcVerticlesClient::class.java)
  }

  private val verticles: MutableList<Verticle> = mutableListOf()

  // TODO add pair publisher / projector with a clustered singleton verticle managing local instances of these 2 verticles

  /**
   * Creates a EventsPublisherVerticle
   */
  fun addEventsPublisher(
    projection: String,
    options: EventsPublisherOptions
  ) {
    val eventsScanner = PgcEventsScanner(pgcClient.sqlClient, projection)
    // consider an additional optional param to filter events
    val v = EventsPublisherVerticle(
      eventsScanner,
      EventBusPublisher(options.targetEndpoint, pgcClient.vertx.eventBus()), options
    )
    verticles.add(v)
  }

  /**
   * Creates a PgcEventsProjectorVerticle
   */
  fun <E : DomainEvent> addEventsProjector(
    endpoint: String,
    eventsProjector: PgcEventsProjector<E>
  ) {
    val v = PgcEventsProjectorVerticle(pgcClient.json, pgcClient.pgPool, eventsProjector, endpoint)
    verticles.add(v)
  }

  /**
   * Deploy verticles
   */
  fun deployVerticles(opt: DeploymentOptions = DeploymentOptions()): Future<Void> {
    val promise = Promise.promise<Void>()
    val initialFuture = Future.succeededFuture<String>()
    foldLeft(
      verticles.iterator(),
      initialFuture,
      { currentFuture: Future<String>, verticle: Verticle ->
        currentFuture.compose {
          log.info("Deploying {}", verticle::class.java.name)
          pgcClient.vertx.deployVerticle(verticle)
        }
      }
    ).onComplete {
      if (it.failed()) {
        promise.fail(it.cause())
      } else {
        promise.complete()
      }
    }
    return promise.future()
  }

  private fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
    var result = identity
    while (iterator.hasNext()) {
      val next = iterator.next()
      result = bf.apply(result, next)
    }
    return result
  }
}
