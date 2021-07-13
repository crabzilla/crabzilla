package io.github.crabzilla.spi

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerticleFactoryTest {

  @Test
  @DisplayName("deploying verticle")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val config = JsonObject().put("name", "john")
    val options = DeploymentOptions().setConfig(config)
    vertx.deployVerticle("service:crabzilla.TestVerticle", options)
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { deploymentId ->
        tc.verify {
          assertThat(deploymentId).isNotNull()
        }
        tc.completeNow()
      }
  }

  @Test
  fun mapEmpty() {
    println(Future.succeededFuture<Void>().mapEmpty<Void>())
    println(Future.failedFuture<Void>(IllegalArgumentException("")).mapEmpty<Void>())
  }
}
