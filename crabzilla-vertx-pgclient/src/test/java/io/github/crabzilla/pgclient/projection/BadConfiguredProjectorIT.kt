package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.command.CommandsContext
import io.github.crabzilla.pgclient.command.pgPool
import io.github.crabzilla.pgclient.projection.infra.TestRepository
import io.github.crabzilla.pgclient.projection.infra.cleanDatabase
import io.github.crabzilla.pgclient.projection.infra.config
import io.github.crabzilla.pgclient.projection.infra.deployVerticles
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class BadConfiguredProjectorIT {

  lateinit var jsonSerDer: JsonSerDer
  lateinit var commandsContext: CommandsContext
  private lateinit var testRepo: TestRepository

  @Test
  @DisplayName("should not deploy since bad config")
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    val pgPool = pgPool(vertx)
    commandsContext = CommandsContext(vertx, jsonSerDer, pgPool)
    testRepo = TestRepository(pgPool)
    val verticles = listOf(
      "service:crabzilla.example1.customer.BadConfiguredEventsProjector",
    )
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(pgPool)
      .compose { vertx.deployVerticles(verticles, options) }
      .onFailure { tc.completeNow() }
      .onSuccess { tc.failNow("Should fail") }
  }
}
