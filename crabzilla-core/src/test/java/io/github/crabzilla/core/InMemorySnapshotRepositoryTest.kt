package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerJson
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class InMemorySnapshotRepositoryTest {

  @Test
  @DisplayName("given none snapshot, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val repo =
      InMemorySnapshotRepository<Customer, CustomerCommand, CustomerEvent>(vertx.sharedData(), customerJson, "customer")
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
      InMemorySnapshotRepository<Customer, CustomerCommand, CustomerEvent>(vertx.sharedData(), customerJson, "customer")
    repo.upsert(1, Snapshot(Customer(id = 1, name = "c1", isActive = false), 1))
      .onFailure { err -> tc.failNow(err) }
      .onSuccess {
        repo.get(1).onComplete { event2 ->
          if (event2.failed()) {
            event2.cause().printStackTrace()
            tc.failNow(event2.cause())
            return@onComplete
          }
          val snapshot: Snapshot<Customer>? = event2.result()
          tc.verify { assertThat(snapshot!!.version).isEqualTo(1) }
          tc.verify { assertThat(snapshot!!.state).isEqualTo(Customer(1, "c1", false, null)) }
          tc.completeNow()
        }
      }
  }
}
