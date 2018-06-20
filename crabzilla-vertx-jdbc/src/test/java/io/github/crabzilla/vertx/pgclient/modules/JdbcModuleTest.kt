package io.github.crabzilla.vertx.modules

import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.modules.test.DaggerTestComponent
import io.github.crabzilla.vertx.modules.test.TestModule
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class JdbcModuleTest {

  @Test
  fun load(vertx: Vertx, tc: VertxTestContext) {

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "example1.env"))

    configHandler(vertx, envOptions, { config ->

      vertx.executeBlocking<Any>({ future ->

        val component = DaggerTestComponent.builder()
          .testModule(TestModule(vertx, config))
          .build()

        assertNotNull(component.healthCheckHandler())

        future.complete()

      }, { res ->

        if (res.failed()) {
          tc.failNow(res.cause())
        }
        tc.completeNow()

      })

    }, {
      tc.completeNow()
    })

  }

}
