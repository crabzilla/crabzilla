package io.github.crabzilla.pgc

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

  override fun start() {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
          .onFailure {
            log.error("Cleaning db", it)
            //promise.fail(it)
          }
          .onSuccess {
            log.info("Success")
            //promise.complete()
            return@onSuccess
//            val publisherVerticle = PgcPoolingProjectionVerticle(
//              "customers", writeDb,
//              EventBusEventsPublisher(topic, vertx.eventBus()), defaultInterval
//            )
//            val projectorVerticle = CustomerProjectorVerticle(customerJson, CustomerRepository(readDb))
//            log.info("Will deploy publisherVerticle")
//            vertx.deployVerticle(publisherVerticle)
//              .onFailure { err ->
//                log.error("When deploying publisherVerticle", it)
//                promise.fail(err)
//              }
//              .onSuccess {
//                log.info("publisherVerticle started")
//                promise.complete()
//                log.info("Now will deploy projectorVerticle")
//                vertx.deployVerticle(projectorVerticle)
//                  .onFailure {
//                    log.error("When deploying projectorVerticle", it)
//                    promise.fail(it)
//                  }
//                  .onSuccess {
//                    log.info("projectorVerticle started")
//                    promise.complete()
//                  }
//              }
          }
      }
  }

  override fun stop() {
    log.info("Stopped")
  }
}
