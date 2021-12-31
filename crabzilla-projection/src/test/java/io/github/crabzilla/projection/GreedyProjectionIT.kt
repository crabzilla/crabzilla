package io.github.crabzilla.projection

import io.github.crabzilla.command.CommandsContext
import io.github.crabzilla.command.SnapshotType
import io.github.crabzilla.core.EventTopics
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.projection.infra.TestRepository
import io.github.crabzilla.projection.infra.cleanDatabase
import io.github.crabzilla.projection.infra.config
import io.github.crabzilla.projection.infra.connectOptions
import io.github.crabzilla.projection.infra.poolOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class GreedyProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(GreedyProjectionIT::class.java)
  }

  private val id: UUID = UUID.randomUUID()
  lateinit var jsonSerDer: JsonSerDer
  lateinit var commandsContext: CommandsContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    testRepo = TestRepository(commandsContext.pgPool)

    cleanDatabase(commandsContext.pgPool)
      .compose {
        deployProjector(
          log, vertx, config,
          "service:crabzilla.example1.customer.CustomersEventsProjector"
        )
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("closing db connections")
  fun cleanup(tc: VertxTestContext) {
    commandsContext.close()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("forcing it starting greedy then sending a command the events will be projected")
  fun a0(tc: VertxTestContext, vertx: Vertx) {

    commandsContext.pgPool
      .preparedQuery("NOTIFY " + EventTopics.STATE_TOPIC.name.lowercase() + ", 'Customer'")
      .execute()
      .onFailure { tc.failNow(it) }
      .onSuccess {
        val target = "crabzilla.example1.customer.CustomersEventsProjector"
        val controller = commandsContext.create(customerConfig, SnapshotType.ON_DEMAND)
        controller.handle(CommandMetadata(StateId(id)), RegisterCustomer(id, "cust#$id"))
          .compose {
            vertx.eventBus()
              .request<String>("crabzilla.projectors.$target.ping", "me")
          }.compose {
            vertx.eventBus().request<Void>("crabzilla.projectors.$target", null)
          }.compose {
            commandsContext.pgPool.preparedQuery("select * from customer_summary")
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
}
