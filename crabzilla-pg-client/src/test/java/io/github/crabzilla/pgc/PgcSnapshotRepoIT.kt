package io.github.crabzilla.pgc

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

// TODO given a snapshot and none events
// TODO given a snapshot and some events, etc

@ExtendWith(VertxExtension::class)
class PgcSnapshotRepoIT {

  private lateinit var writeDb: PgPool
  private lateinit var repo: PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: PgcEventRepoTestHelper

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        repo = PgcSnapshotRepo(customerConfig, writeDb)
        testRepo = PgcEventRepoTestHelper(writeDb)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("given none snapshot, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    repo.get(UUID.randomUUID())
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot).isNull() }
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("it can retrieve correct snapshot")
  fun a1(tc: VertxTestContext, vertx: Vertx) {
    val id = UUID.randomUUID()
    val snapshotAsJson = JsonObject()
      .put("type", "Customer")
      .put("id", id.toString())
      .put("name", "c1")
      .put("isActive", false)
    testRepo.upsert(id, customerConfig.name, 1, snapshotAsJson)
      .compose { repo.get(id) }
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot!!.version).isEqualTo(1) }
        tc.verify { assertThat(snapshot!!.state).isEqualTo(Customer(id, "c1", false, null)) }
        tc.completeNow()
      }
  }
}
