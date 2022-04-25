package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomersPgEventProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.pgPoolOptions
import io.github.crabzilla.stack.command.CommandControllerOptions
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.pubsub.PgSubscriber
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertTimeout
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ProjectingToPostgresIT {

  companion object {
    const val projectionName = "crabzilla.example1.customer.SimpleProjector"
  }

  private val projectorEndpoints = ProjectorEndpoints(projectionName)
  private val id: UUID = UUID.randomUUID()

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can project to postgres within an interval`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 10L)
    val controller = CommandController.createAndStart(
      vertx = vertx,
      pgPool = pgPool,
      json = TestsFixtures.json,
      commandComponent = customerConfig,
      options = options
    )
    val config = ProjectorConfig(projectionName, initialInterval = 10, interval = 10)
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, config, CustomersPgEventProjector())
    component.start()
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose {
            pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
          }.onFailure {
            tc.failNow(it)
          }.onSuccess {
            tc.verify {
              assertTimeout(Duration.ofMillis(1000)) { it }
              tc.completeNow()
            }
          }
      }
  }

  @Test
  @Order(2)
  fun `it can project to postgres when explicit calling it`(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig)
    val options = ProjectorConfig(projectionName)
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, options, CustomersPgEventProjector())
    component.start()
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
          .compose {
            pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
          }.onFailure {
            tc.failNow(it)
          }.onSuccess {
            tc.verify {
              assertTimeout(Duration.ofMillis(1000)) { it }
              tc.completeNow()
            }
          }
      }
  }
}
