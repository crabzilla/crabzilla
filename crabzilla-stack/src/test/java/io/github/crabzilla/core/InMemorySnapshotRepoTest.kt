package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.stack.InMemorySnapshotRepo
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class InMemorySnapshotRepoTest {

  @Test
  @DisplayName("given none snapshot, it must retrieve a null snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val repo =
      InMemorySnapshotRepo<Customer, CustomerCommand, CustomerEvent>(vertx.sharedData(), customerConfig)
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
      InMemorySnapshotRepo<Customer, CustomerCommand, CustomerEvent>(vertx.sharedData(), customerConfig)
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
