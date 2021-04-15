package io.github.crabzilla.pgc

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.InMemorySnapshotRepo
import io.github.crabzilla.stack.PoolingProjectionVerticle
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

@ExtendWith(VertxExtension::class)
class PgcFullStackIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(PgcFullStackIT::class.java)
    const val topic = "customers"
  }

  val id = (0..10_000).random()

  lateinit var writeDb: PgPool
  lateinit var readDb: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .onFailure { tc.failNow(it.cause) }
      .compose { config ->
        cleanDatabase(vertx, config)
          .onFailure { tc.failNow(it.cause) }
          .compose {
            writeDb = writeModelPgPool(vertx, config)
            readDb = readModelPgPool(vertx, config)
            val projectorVerticle = CustomerProjectorVerticle(customerJson, CustomerRepository(readDb))
            val eventsScanner = PgcEventsScanner(writeDb, topic)
            val publisherVerticle = PoolingProjectionVerticle(
              eventsScanner, EventBusEventsPublisher(topic, vertx.eventBus()), 120_000
            )
            vertx.deployVerticle(projectorVerticle)
              .compose { vertx.deployVerticle(publisherVerticle) }
              .onFailure { tc.failNow(it.cause) }
              .onSuccess {
                log.info("Success")
                tc.completeNow()
              }
          }
      }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val snapshotRepo = PgcSnapshotRepo(customerConfig, writeDb)
    val controller = CommandControllerFactory.createPublishingTo(topic, customerConfig, writeDb, snapshotRepo)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            vertx.eventBus().request<Boolean>(PoolingProjectionVerticle.PUBLISHER_ENDPOINT, null) { it ->
              if (it.failed()) {
                tc.failNow(it.cause())
              } else {
                snapshotRepo.get(id)
                  .onFailure { err -> tc.failNow(err) }
                  .onSuccess { snapshot1 ->
                    assert(1 == snapshot1!!.version)
                    assert(Customer(id, "cust#$id") == snapshot1.state)
                    controller.handle(CommandMetadata(id), CustomerCommand.ActivateCustomer("because yes"))
                      .onFailure { tc.failNow(it) }
                      .onSuccess {
                        vertx.eventBus().request<Boolean>(PoolingProjectionVerticle.PUBLISHER_ENDPOINT, null) { it ->
                          if (it.failed()) {
                            tc.failNow(it.cause())
                          } else {
                            snapshotRepo.get(id)
                              .onFailure { err -> tc.failNow(err) }
                              .onSuccess { snapshot2 ->
                                assert(2 == snapshot2!!.version)
                                assert(Customer(id, "cust#$id", isActive = true, reason = "because yes") == snapshot2.state)
                                tc.completeNow()
                              }
                          }
                        }
                      }
                  }
              }
            }
          }
      }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using in memory snapshot repo")
  fun a1(tc: VertxTestContext, vertx: Vertx) {
    val snapshotRepo = InMemorySnapshotRepo(vertx.sharedData(), customerConfig)
    val controller = CommandControllerFactory.createPublishingTo(topic, customerConfig, writeDb, snapshotRepo)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            vertx.eventBus().request<Boolean>(PoolingProjectionVerticle.PUBLISHER_ENDPOINT, null) { it ->
              if (it.failed()) {
                tc.failNow(it.cause())
              } else {
                snapshotRepo.get(id)
                  .onFailure { err -> tc.failNow(err) }
                  .onSuccess { snapshot1 ->
                    assert(1 == snapshot1!!.version)
                    assert(Customer(id, "cust#$id") == snapshot1.state)
                    controller.handle(CommandMetadata(id), CustomerCommand.ActivateCustomer("because yes"))
                      .onFailure { tc.failNow(it) }
                      .onSuccess {
                        vertx.eventBus().request<Boolean>(PoolingProjectionVerticle.PUBLISHER_ENDPOINT, null) { it ->
                          if (it.failed()) {
                            tc.failNow(it.cause())
                          } else {
                            snapshotRepo.get(id)
                              .onFailure { err -> tc.failNow(err) }
                              .onSuccess { snapshot2 ->
                                assert(2 == snapshot2!!.version)
                                assert(
                                  Customer(
                                    id,
                                    "cust#$id",
                                    isActive = true,
                                    reason = "because yes"
                                  ) == snapshot2.state
                                )
                                tc.completeNow()
                              }
                          }
                        }
                      }
                  }
              }
            }
          }
      }
  }
}
