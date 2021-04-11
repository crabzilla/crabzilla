package io.github.crabzilla.pgc

import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.customerJson
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory

class CustomerVerticle(private val defaultInterval: Long) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerVerticle::class.java)
    const val topic = "customers"
  }

  lateinit var writeDb: PgPool
  lateinit var readDb: PgPool

  override fun start() {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        val eventsScanner = PgcEventsScanner(writeDb, topic)
        val publisherVerticle = PgcPoolingProjectionVerticle(
          eventsScanner, EventBusEventsPublisher(topic, vertx.eventBus()), defaultInterval
        )
        val projectorVerticle = CustomerProjectorVerticle(customerJson, CustomerRepository(readDb))
        log.info("Will deploy publisherVerticle")
        vertx.deployVerticle(publisherVerticle)
          .compose { vertx.deployVerticle(projectorVerticle) }
          .onFailure {
            log.error("When deploying verticles", it)
          }
          .onSuccess {
            log.info("verticles started")
            // promise.complete()
          }
      }
  }

  override fun stop() {
    log.info("Stopped")
  }
}
