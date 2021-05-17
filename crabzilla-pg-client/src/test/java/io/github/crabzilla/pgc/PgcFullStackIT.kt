package io.github.crabzilla.pgc

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.pgc.CustomerProjectorVerticle.Companion.topic
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventsPublisherVerticle
import io.github.crabzilla.stack.EventsPublisherVerticleOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class PgcFullStackIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(PgcFullStackIT::class.java)
  }

  val id = UUID.randomUUID()

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
            val options = EventsPublisherVerticleOptions.Builder()
              .targetEndpoint(topic)
              .eventBus(vertx.eventBus())
              .interval(500)
              .maxInterval(60_000)
              .maxNumberOfRows(1)
              .statsInterval(30_000)
              .build()
            val publisherVerticle = EventsPublisherVerticleFactory.create(topic, writeDb, options)
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
    val controller = CommandControllerFactory.create(customerConfig, writeDb)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(AggregateRootId(id)), RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            vertx.eventBus().request<Boolean>(EventsPublisherVerticle.PUBLISHER_ENDPOINT, null) { it ->
              if (it.failed()) {
                tc.failNow(it.cause())
              } else {
                snapshotRepo.get(id)
                  .onFailure { err -> tc.failNow(err) }
                  .onSuccess { snapshot1 ->
                    assert(1 == snapshot1!!.version)
                    assert(Customer(id, "cust#$id") == snapshot1.state)
                    controller.handle(CommandMetadata(AggregateRootId(id)), ActivateCustomer("because yes"))
                      .onFailure { tc.failNow(it) }
                      .onSuccess {
                        vertx.eventBus().request<Boolean>(EventsPublisherVerticle.PUBLISHER_ENDPOINT, null) {
                          if (it.failed()) {
                            tc.failNow(it.cause())
                          } else {
                            snapshotRepo.get(id)
                              .onFailure { err -> tc.failNow(err) }
                              .onSuccess { snapshot2 ->
                                assert(2 == snapshot2!!.version)
                                assert(
                                  Customer(id, "cust#$id", isActive = true, reason = "because yes")
                                    == snapshot2.state
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
