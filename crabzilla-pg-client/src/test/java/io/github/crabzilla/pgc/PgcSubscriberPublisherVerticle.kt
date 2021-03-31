package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventsPublisher
import io.vertx.core.Promise
import io.vertx.pgclient.pubsub.PgSubscriber
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * This component will be triggered using a Postgres LISTEN command. Then it can publish the domain events to
 * a EventsPublisher.
 */
class PgcSubscriberPublisherVerticle(
  private val channel: String,
  private val pgSubscriber: PgSubscriber,
  eventsScanner: PgcEventsScanner,
  eventsPublisher: EventsPublisher,
  private val numberOfRows: Int = NUMBER_OF_ROWS
) : AbstractPublisherVerticle(eventsScanner, eventsPublisher) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcSubscriberPublisherVerticle::class.java)
    private const val NUMBER_OF_ROWS = 1000
  }

  private val notifications = AtomicLong()

  override fun start(promise: Promise<Void>) {
    pgSubscriber.connect {
      if (it.failed()) {
        log.error("When connecting to a subscriber", it.cause())
        promise.fail(it.cause())
        return@connect
      }
      promise.complete()
      log.info("Connected and listening to channel [$channel]")
      pgSubscriber.channel(channel)
        .subscribeHandler {
          log.info("Received a notification #${notifications.incrementAndGet()} from channel [$channel]")
          scanAndPublish(numberOfRows)
        }
    }
  }

  override fun stop() {
    log.info("Stopped")
  }
}
