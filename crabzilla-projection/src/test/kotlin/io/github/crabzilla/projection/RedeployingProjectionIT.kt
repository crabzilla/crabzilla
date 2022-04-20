package io.github.crabzilla.projection

import io.github.crabzilla.TestRepository
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.config
import io.github.crabzilla.pgPool
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@DisplayName("Deploying events projector")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RedeployingProjectionIT {

  lateinit var pgPool: PgPool
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `if it's not deployed, it will deploy`(tc: VertxTestContext, vertx: Vertx) {
    vertx.deployProjector(
      config, "service:crabzilla.example1.customer.CustomersEventsProjector"
    )
      .onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }

  @Test
  @Order(2)
  fun `if it's already deployed, it will keep the current instance`(tc: VertxTestContext, vertx: Vertx) {
    vertx.deployProjector(
      config, "service:crabzilla.example1.customer.CustomersEventsProjector"
    ).compose {
      vertx.deployProjector(
        config, "service:crabzilla.example1.customer.CustomersEventsProjector"
      )
    }
      .onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }
}
