package io.github.crabzilla.pgc.api

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEventsProjector
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.cleanDatabase
import io.github.crabzilla.pgc.connectOptions
import io.github.crabzilla.pgc.poolOptions
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandMetadata
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
class PgcClientSyncProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(PgcClientSyncProjectionIT::class.java)
  }

  val id = UUID.randomUUID()

  lateinit var client: PgcClient

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = PgcClient.create(vertx, customerJson, connectOptions, poolOptions)
    cleanDatabase(client.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val commandClient = PgcCommandClient(client)
    val snapshotRepo = PgcSnapshotRepo<Customer>(client.pgPool, client.json)
    val controller = commandClient.create(customerConfig, true, CustomerEventsProjector)
    snapshotRepo.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(AggregateRootId(id)), RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            snapshotRepo.get(id)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { snapshot1 ->
                assert(1 == snapshot1!!.version)
                assert(snapshot1.state == Customer(id, "cust#$id"))
                controller.handle(CommandMetadata(AggregateRootId(id)), ActivateCustomer("because yes"))
                  .onFailure { tc.failNow(it) }
                  .onSuccess {
                    snapshotRepo.get(id)
                      .onFailure { err -> tc.failNow(err) }
                      .onSuccess { snapshot2 ->
                        assert(2 == snapshot2!!.version)
                        val expected = Customer(id, "cust#$id", isActive = true, reason = "because yes")
                        assert(snapshot2.state == expected)
                        tc.completeNow()
                      }
                  }
              }
          }
      }
  }
}
