package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.cleanDatabase
import io.github.crabzilla.pgclient.command.config
import io.github.crabzilla.pgclient.command.pgPool
import io.github.crabzilla.pgclient.deployProjector
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
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
@DisplayName("Deploying events projector")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RedeployingProjectionIT {

  lateinit var jsonSerDer: JsonSerDer
  lateinit var pgPool: PgPool
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
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
  fun `if it's already deployed, it will keep going`(tc: VertxTestContext, vertx: Vertx) {
    vertx.deployProjector(
      config, "service:crabzilla.example1.customer.CustomersEventsProjector"
    )
      .onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }

}
