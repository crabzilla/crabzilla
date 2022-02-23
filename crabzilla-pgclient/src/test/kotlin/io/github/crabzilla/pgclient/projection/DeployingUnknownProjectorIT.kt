package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.cleanDatabase
import io.github.crabzilla.pgclient.command.config
import io.github.crabzilla.pgclient.command.pgPool
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@DisplayName("Deploying an invalid events projector")
class DeployingUnknownProjectorIT {

  private lateinit var testRepo: TestRepository

  @Test
  fun `deploying an invalid projector`(vertx: Vertx, tc: VertxTestContext) {
    val pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(pgPool)
      .compose { vertx.deployVerticle("service:?", options) }
      .onFailure { tc.completeNow() }
      .onSuccess { tc.failNow("Should fail") }
  }
}
