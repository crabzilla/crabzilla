package io.github.crabzilla.context

import io.github.crabzilla.TestRepository
import io.github.crabzilla.example1.customer.CustomerEvent
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@DisplayName("Instantiating CrabzillaContext")
@ExtendWith(VertxExtension::class)
class CrabzillaContextTest {
  @Test
  fun `it can instantiate context`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, TestRepository.testDbConfig)
    assertThat(context).isNotNull()
  }

  @Test
  fun `it start a transaction`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, TestRepository.testDbConfig)
    context.withinTransaction { tx ->
      tx.query("SELECT 1").execute().map {
        val eventMetadata =
          EventMetadata(
            streamId = 1,
            stateType = "Customer",
            stateId = context.uuidFunction.invoke().toString(),
            eventId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
            eventSequence = 1,
            version = 1,
            eventType = CustomerEvent.CustomerRegistered::class.java.simpleName,
          )
        eventMetadata
      }
    }
  }

  @Test
  fun `it create s subscriber`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, TestRepository.testDbConfig)
    assertThat(context.newPgSubscriber()).isNotNull
  }
}
