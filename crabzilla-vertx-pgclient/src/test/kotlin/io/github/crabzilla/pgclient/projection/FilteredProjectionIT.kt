package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.EventTopics
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.example1Json
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.CommandController
import io.github.crabzilla.pgclient.command.SnapshotType
import io.github.crabzilla.pgclient.command.cleanDatabase
import io.github.crabzilla.pgclient.command.config
import io.github.crabzilla.pgclient.command.pgPool
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@DisplayName("Deploying a filtered events projector")
class FilteredProjectionIT {

  private val id: UUID = UUID.randomUUID()
  lateinit var pgPool: PgPool
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    pgPool = pgPool(vertx)
    testRepo = TestRepository(pgPool)

    cleanDatabase(pgPool)
      .compose {
        vertx.deployProjector(config, "service:crabzilla.example1.customer.FilteredEventsProjector")
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // TODO test idempotency

  @Test
//  @Disabled // TODO fixme
  @DisplayName("after a command the events will be projected")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val target = "crabzilla.example1.customer.FilteredEventsProjector"
    val controller = CommandController.create(vertx, pgPool, example1Json, customerConfig, SnapshotType.ON_DEMAND)
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        Thread.sleep(500L)
        vertx.eventBus()
          .request<String>("crabzilla.projectors.$target.ping", "me")
      }.compose {
        Thread.sleep(500L)
        vertx.eventBus().request<Void>("crabzilla.projectors.$target", null)
      }.compose {
        pgPool
          .preparedQuery("NOTIFY " + EventTopics.STATE_TOPIC.name.lowercase() + ", 'Customer'")
          .execute()
      }.compose {
        pgPool
          .preparedQuery("NOTIFY " + EventTopics.STATE_TOPIC.name.lowercase() + ", 'Customer'")
          .execute()
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
