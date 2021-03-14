package io.github.crabzilla.pgc

import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerEventHandler
import io.github.crabzilla.example1.customerJson
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

// TODO given a snapshot and none events
// TODO given a snapshot and some events, etc

@ExtendWith(VertxExtension::class)
class PgcSnapshotRepoIT {

  private lateinit var writeDb: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("given none snapshot, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val repo =
      PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>(
        customerEventHandler, Customer::class::simpleName.name,
        "customer_snapshots", writeDb, customerJson
      )
    repo.get(-1)
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot).isNull() }
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("when appending it can retrieve correct snapshot")
  fun a1(tc: VertxTestContext, vertx: Vertx) {
    val repo =
      PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>(
        customerEventHandler, Customer::class::simpleName.name,
        "customer_snapshots", writeDb, customerJson
      )
    repo.upsert(1, Snapshot(Customer(id = 1, name = "c1", isActive = false), 1))
      .compose { repo.get(1) }
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot!!.version).isEqualTo(1) }
        tc.verify { assertThat(snapshot!!.state).isEqualTo(Customer(1, "c1", false, null)) }
        tc.completeNow()
      }
  }
}
