package io.github.crabzilla.pgc

import io.github.crabzilla.example1.customerConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class CommandControllerFactoryTests {

  private lateinit var writeDb: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { config ->
        writeDb = writeModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
          .onSuccess {
            tc.completeNow()
            println("ok")
          }
          .onFailure { err ->
            tc.failNow(err)
            err.printStackTrace()
          }
      }
  }

  @Test
  @DisplayName("it can create a command controller")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandControllerFactory.create(customerConfig, writeDb)
    assertThat(controller).isNotNull
    tc.completeNow()
  }
}
