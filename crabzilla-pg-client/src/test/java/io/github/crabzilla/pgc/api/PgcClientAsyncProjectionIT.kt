package io.github.crabzilla.pgc.api

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEventsProjector
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.cleanDatabase
import io.github.crabzilla.pgc.connectOptions
import io.github.crabzilla.pgc.poolOptions
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventsPublisherOptions
import io.github.crabzilla.stack.EventsPublisherVerticle
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class PgcClientAsyncProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(PgcClientAsyncProjectionIT::class.java)
  }

  val id = UUID.randomUUID()

  lateinit var client: PgcClient

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = PgcClient.create(vertx, customerJson, connectOptions, poolOptions)
    val verticlesClient = PgcVerticlesClient(client)
    verticlesClient.addEventsPublisher("customers", options)
    verticlesClient.addEventsProjector(options.targetEndpoint, CustomerEventsProjector)
    cleanDatabase(client.sqlClient)
      .compose { verticlesClient.deployVerticles() }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val commandClient = PgcCommandClient(client)
    val snapshotRepo = PgcSnapshotRepo<Customer>(client.pgPool, client.json)
    val controller = commandClient.create(customerConfig, true)
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
                    controller.handle(
                      CommandMetadata(AggregateRootId(id)),
                      ActivateCustomer("because yes")
                    )
                      .onFailure { tc.failNow(it) }
                      .onSuccess {
                        vertx.eventBus()
                          .request<Boolean>(
                            EventsPublisherVerticle.PUBLISHER_ENDPOINT,
                            null
                          ) {
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
                                    )
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

  private val options = EventsPublisherOptions.Builder()
    .targetEndpoint("customers-events")
    .interval(500)
    .maxInterval(60_000)
    .maxNumberOfRows(1)
    .statsInterval(30_000)
    .build()
}
