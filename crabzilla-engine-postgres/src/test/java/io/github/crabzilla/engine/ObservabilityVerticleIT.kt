package io.github.crabzilla.engine

import io.github.crabzilla.stack.ObservabilityVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ObservabilityVerticleIT {

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    vertx.deployVerticle(ObservabilityVerticle(10000))
      .onSuccess { tc.completeNow() }
      .onFailure { tc.failNow(it) }
  }

  @Test
  @DisplayName("projections")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val json = JsonObject()
      .put("projectionId", "projection-1")
      .put("sequence", 31)
    vertx.eventBus().request<Void>("crabzilla.projections", json)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("publications")
  fun a1(tc: VertxTestContext, vertx: Vertx) {
    val json = JsonObject()
      .put("publicationId", "publication-1")
      .put("sequence", 41)
    vertx.eventBus().request<Void>("crabzilla.publications", json)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("commands")
  fun a3(tc: VertxTestContext, vertx: Vertx) {
    val json = JsonObject()
      .put("controllerId", "Customer")
      .put("successes", 431)
      .put("failures", 0)
    vertx.eventBus().request<Void>("crabzilla.command-controllers", json)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }
}
