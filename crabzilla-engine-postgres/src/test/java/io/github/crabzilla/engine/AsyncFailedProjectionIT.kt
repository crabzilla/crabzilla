package io.github.crabzilla.engine

import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.serder.KotlinSerDer
import io.github.crabzilla.serder.SerDer
import io.github.crabzilla.stack.StateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.deployVerticles
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class AsyncFailedProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(AsyncFailedProjectionIT::class.java)
  }

  val id = UUID.randomUUID()
  lateinit var serDer: SerDer
  lateinit var client: CommandsContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    serDer = KotlinSerDer(example1Json)
    client = CommandsContext.create(vertx, serDer, connectOptions, poolOptions)
    testRepo = TestRepository(client.pgPool)
    val verticles = listOf(
      "service:crabzilla.example1.customer.CustomersEventsPublisher" // only publisher
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
    val snapshotRepo = SnapshotRepository<Customer>(client.pgPool, example1Json)
    val controller = client.create(customerConfig)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(StateId(id)), RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            snapshotRepo.get(id)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { snapshot1 ->
                assert(1 == snapshot1!!.version)
                assert(Customer(id, "cust#$id") == snapshot1.state)
                controller.handle(
                  CommandMetadata(StateId(id)),
                  ActivateCustomer("because yes")
                )
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
                        testRepo.getProjections("customers")
                          .onFailure { tc.failNow(it) }
                          .onSuccess {
                            tc.verify {
                              assertThat(it).isEqualTo(0)
                            }
                            tc.completeNow()
                          }
                      }
                  }
              }
          }
      }
  }
}
