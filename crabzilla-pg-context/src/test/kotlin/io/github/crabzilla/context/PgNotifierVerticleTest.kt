package io.github.crabzilla.context

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@DisplayName("PgNotifierVerticle")
@ExtendWith(VertxExtension::class)
class PgNotifierVerticleTest : AbstractContextTest() {
  @Test
  fun `it works`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val context = CrabzillaContextImpl(vertx, dbConfig)

    val latch = CountDownLatch(1)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = context.newPgSubscriber()
    pgSubscriber.connect().onSuccess {
      pgSubscriber.channel(CrabzillaContext.POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          println("-----> $stateType")
          stateTypeMsg.set(stateType)
          latch.countDown()
        }
    }

    val verticle = PgNotifierVerticle(context.pgPool, intervalMilliseconds = 5, maxInterval = 1000)

    vertx.deployVerticle(verticle)
      .compose { vertx.eventBus().request<Void>(PgNotifierVerticle.PG_NOTIFIER_ADD_ENDPOINT, "state-type-x") }
      .compose { vertx.eventBus().request<Void>(PgNotifierVerticle.PG_NOTIFIER_ADD_ENDPOINT, "state-type-x") }
      .compose { vertx.eventBus().request<Void>(PgNotifierVerticle.PG_NOTIFIER_ADD_ENDPOINT, "state-type-x") }
      .compose { vertx.eventBus().request<Void>(PgNotifierVerticle.PG_NOTIFIER_ADD_ENDPOINT, "state-type-x") }

    tc.verify {
      Assertions.assertTrue(latch.await(4, TimeUnit.SECONDS))
      assertThat(stateTypeMsg.get()).isEqualTo("state-type-x")
      tc.completeNow()
    }
  }
}
