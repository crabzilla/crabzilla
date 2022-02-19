package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.EventTopics
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.cleanDatabase
import io.github.crabzilla.pgclient.command.config
import io.github.crabzilla.pgclient.command.pgPool
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@DisplayName("Forcing projector to be greedy")
class GreedyProjectionIT {

  lateinit var pgPool: PgPool
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .compose {
        vertx.deployProjector(
          config, "service:crabzilla.example1.customer.CustomersSlowEventsProjector"
        )
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("forcing it starting greedy then sending a command the events will be projected")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val target = "crabzilla.example1.customer.CustomersSlowEventsProjector"
    pgPool
      .preparedQuery("NOTIFY " + EventTopics.STATE_TOPIC.name.lowercase() + ", 'Customer'")
      .execute()
      .compose {
        Thread.sleep(500)
        vertx.eventBus().request<JsonObject>("crabzilla.projectors.$target.status", null)
      }.onSuccess {
        println(it.body().encodePrettily())
        if (it.body().getBoolean("greedy")) {
          tc.completeNow()
        } else {
          tc.failNow("Status should be greedy")
        }
      }
  }
}
