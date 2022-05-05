package io.github.crabzilla.projection

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
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
  private lateinit var context : CrabzillaContext

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can project to postgres within an interval`(tc: VertxTestContext, vertx: Vertx) {
    val options = FeatureOptions(pgNotificationInterval = 100L)
    val controller = context.featureController(customerComponent, jsonSerDer, options)
    val config = ProjectorConfig(projectionName, initialInterval = 10, interval = 100)
    val projector = context.postgresProjector(config, CustomersEventProjector())
    vertx.deployVerticle(projector)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose {
            context.pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
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
    val controller = context.featureController(customerComponent, jsonSerDer)
    val config = ProjectorConfig(projectionName)
    val projector = context.postgresProjector(config, CustomersEventProjector())
    vertx.deployVerticle(projector)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.handle(), null) }
          .compose {
            context.pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
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
