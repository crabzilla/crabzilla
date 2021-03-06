package io.github.crabzilla.pgc

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerCommandVerticle
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.command.CommandControllerClient
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.stack.DomainStateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.deployVerticles
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class CustomerCommandVerticleIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(CustomerCommandVerticleIT::class.java)
  }

  val id = UUID.randomUUID()

  lateinit var client: CommandControllerClient

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = CommandControllerClient.create(vertx, example1Json, connectOptions, poolOptions)
    val verticles = listOf(
//      "service:crabzilla.example1.customer.CustomersEventsPublisher",
//      "service:crabzilla.example1.customer.CustomersEventsProjector",
      "service:crabzilla.example1.customer.CustomersCommandVerticle"
    )
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(client.sqlClient)
      .compose { vertx.deployVerticles(verticles, options) }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("closing db connections")
  fun cleanup(tc: VertxTestContext) {
    client.close()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val snapshotRepo = PgcSnapshotRepo<Customer>(client.pgPool, client.json)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        val metadata1 = CommandMetadata(DomainStateId(id))
        val command1 = RegisterCustomer(id, "cust#$id")
        val msg1 = JsonObject()
          .put("metadata", metadata1.toJson())
          .put("command", JsonObject(command1.toJson(example1Json)))
        vertx
          .eventBus()
          .request<Boolean>(CustomerCommandVerticle.ENDPOINT, msg1)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            snapshotRepo.get(id)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { snapshot1 ->
                assert(1 == snapshot1!!.version)
                assert(Customer(id, "cust#$id") == snapshot1.state)

                val metadata2 = CommandMetadata(DomainStateId(id))
                val command2 = ActivateCustomer("because yes")
                val msg2 = JsonObject()
                  .put("metadata", metadata2.toJson())
                  .put("command", JsonObject(command2.toJson(example1Json)))
                vertx
                  .eventBus()
                  .request<Boolean>(CustomerCommandVerticle.ENDPOINT, msg2)
                  .onFailure { tc.failNow(it) }
                  .onSuccess {
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
