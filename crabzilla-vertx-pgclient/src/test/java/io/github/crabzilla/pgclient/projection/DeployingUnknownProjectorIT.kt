package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
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

  lateinit var jsonSerDer: JsonSerDer
  private lateinit var testRepo: TestRepository

  @Test
  fun `deploying an invalid projector`(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    val pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(pgPool)
      .compose { vertx.deployVerticle("service:?", options) }
      .onFailure { tc.completeNow() }
      .onSuccess { tc.failNow("Should fail") }
  }
}
