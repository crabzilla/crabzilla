package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.example1.Example1Fixture.createCmd1
import io.github.crabzilla.example1.Example1Fixture.customerId1
import io.github.crabzilla.example1.Example1Fixture.example1Json
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
  @DisplayName("given none snapshot or event, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val repo = InMemorySnapshotRepository(vertx.sharedData(), example1Json, CUSTOMER_ENTITY, Customer())
    repo.retrieve(customerId1)
        .onFailure { err -> tc.failNow(err) }
        .onSuccess { snapshot ->
        tc.verify { assertThat(snapshot.version).isEqualTo(0) }
        tc.verify { assertThat(snapshot.state).isEqualTo(Customer()) }
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("when appending it can retrieve correct snapshot")
  fun a1(tc: VertxTestContext, vertx: Vertx) {
    val repo = InMemorySnapshotRepository(vertx.sharedData(), example1Json, CUSTOMER_ENTITY, Customer())
    repo.upsert(customerId1, Snapshot(Customer(customerId = customerId1, name = createCmd1.name, isActive = false), 1))
      .onFailure { err -> tc.failNow(err) }
      .onSuccess {
        repo.retrieve(customerId1).onComplete { event2 ->
          if (event2.failed()) {
            event2.cause().printStackTrace()
            tc.failNow(event2.cause())
            return@onComplete
          }
          val snapshot: Snapshot<Customer> = event2.result()
          tc.verify { assertThat(snapshot.version).isEqualTo(1) }
          tc.verify { assertThat(snapshot.state).isEqualTo(Customer(customerId1, createCmd1.name, false, null)) }
          tc.completeNow()
        }
      }
    }
}
