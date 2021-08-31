package io.github.crabzilla.engine

import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.core.serder.KotlinJsonSerDer
import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
class SnapshotRepositoryIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var client: CommandsContext
  private lateinit var repository: SnapshotRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    client = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    repository = SnapshotRepository(client.pgPool, example1Json)
    testRepo = TestRepository(client.pgPool)
    cleanDatabase(client.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("given none snapshot, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    repository.get(UUID.randomUUID())
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
      .compose { repository.get(id) }
      .onFailure { err -> tc.failNow(err) }
      .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot!!.version).isEqualTo(1) }
        tc.verify { assertThat(snapshot!!.state).isEqualTo(Customer(id, "c1", false, null)) }
        tc.completeNow()
      }
  }
}