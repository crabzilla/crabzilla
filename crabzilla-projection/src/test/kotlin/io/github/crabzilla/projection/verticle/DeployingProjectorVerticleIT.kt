package io.github.crabzilla.projection.verticle

import io.github.crabzilla.TestRepository
import io.github.crabzilla.dbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@DisplayName("Deploying projectors verticles")
internal class DeployingProjectorVerticleIT {

  private lateinit var testRepo: TestRepository

  @Test
  @Order(1)
  fun `if it's not deployed, it will deploy`(tc: VertxTestContext, vertx: Vertx) {
    vertx.deployProjector(
      dbConfig, "service:crabzilla.example1.customer.SimpleProjector"
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
      dbConfig, "service:crabzilla.example1.customer.SimpleProjector"
    ).compose {
      vertx.deployProjector(
        dbConfig, "service:crabzilla.example1.customer.SimpleProjector"
      )
    }
      .onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }

  @Test
  fun `deploying an invalid projector`(vertx: Vertx, tc: VertxTestContext) {
    vertx.deployProjector(dbConfig, "service:?")
      .onFailure { tc.completeNow() }
      .onSuccess { tc.failNow("Should fail") }
  }
}
