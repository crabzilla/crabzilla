package io.github.crabzilla.pgc

import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.customerJson
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory

class CustomerVerticle(private val defaultInterval: Long) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerVerticle::class.java)
    const val topic = "example1"
  }

  lateinit var writeDb: PgPool
  lateinit var readDb: PgPool

  override fun start(promise: Promise<Void>) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
          .onSuccess {
            // val publisherVerticle =
            // PgcSubscriberPublisherVerticle(topic, pgSubscriber(vertx, config), pgcEventsScanner, publisherVerticle)
            val publisherVerticle = PgcPoolingPublisherVerticle(
              PgcEventsScanner(writeDb),
              EventBusEventsPublisher(topic, vertx.eventBus()),
              cb(),
              defaultInterval
            )
            val projectorVerticle = CustomerProjectorVerticle(customerJson, CustomerRepository(readDb))
            vertx.deployVerticle(publisherVerticle)
              .compose { vertx.deployVerticle(projectorVerticle) }
              .onFailure { err ->
                promise.fail(err)
              }
              .onSuccess {
                log.info("Started")
                promise.complete()
              }
          }.onFailure { promise.fail(it) }
      }
  }

  override fun stop() {
    log.info("Stopped")
  }

  fun cb(): CircuitBreaker {
    return CircuitBreaker.create(
      "pgc-pooling-circuit-breaker", vertx,
      CircuitBreakerOptions()
        .setMaxFailures(100) // number of failure before opening the circuit
        .setTimeout(1000) // consider a failure if the operation does not succeed in time
        .setFallbackOnFailure(false) // do we call the fallback on failure
        .setResetTimeout(2000) // time spent in open state before attempting to re-try
      // TODO jitter
    ).openHandler {
      log.warn("Circuit opened")
    }.closeHandler {
      log.warn("Circuit closed")
    }.retryPolicy { retryCount -> retryCount * 1000L }
  }
}
