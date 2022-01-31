package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.command.CommandController
import io.github.crabzilla.pgclient.command.SnapshotType
import io.github.crabzilla.pgclient.command.pgPool
import io.github.crabzilla.pgclient.deployProjector
import io.github.crabzilla.pgclient.projection.infra.TestRepository
import io.github.crabzilla.pgclient.projection.infra.cleanDatabase
import io.github.crabzilla.pgclient.projection.infra.config
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class AsyncProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(AsyncProjectionIT::class.java)
  }

  private val id: UUID = UUID.randomUUID()
  lateinit var jsonSerDer: JsonSerDer
  lateinit var pgPool: PgPool
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)

    cleanDatabase(pgPool)
      .compose {
        vertx.deployProjector(config, "service:crabzilla.example1.customer.CustomersEventsProjector")
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("closing db connections")
  fun cleanup(tc: VertxTestContext) {
    pgPool.close()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // TODO test idempotency

  @Test
  @DisplayName("after a command the events will be projected")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val target = "crabzilla.example1.customer.CustomersEventsProjector"
    val controller = CommandController.create(vertx, pgPool, jsonSerDer, customerConfig, SnapshotType.ON_DEMAND)
    controller.handle(CommandMetadata(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        vertx.eventBus()
          .request<String>("crabzilla.projectors.$target.ping", "me")
      }.compose {
        vertx.eventBus().request<Void>("crabzilla.projectors.$target", null)
      }.compose {
        pgPool.preparedQuery("select * from customer_summary")
          .execute()
          .map { rs ->
            rs.size() == 1
          }
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        if (it) {
          tc.completeNow()
        } else {
          tc.failNow("Nothing projected")
        }
      }
  }
}
