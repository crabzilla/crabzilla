package io.github.crabzilla.vertx

import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("ConfigHandler")
@ExtendWith(VertxExtension::class)
class ConfigHandlerTest {

  @Test
  fun load(vertx: Vertx, tc: VertxTestContext) {

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    configHandler(vertx, envOptions, { config ->

      vertx.executeBlocking<Any>({ future ->

        assertThat("example1_write").isEqualTo(config.get("WRITE_DATABASE_NAME"))
        assertThat(10).isEqualTo(config.size())

        future.complete()

      }, { res ->

        if (res.failed()) {
          println(res.cause().message)
          tc.failNow(res.cause())
        }
        tc.completeNow()
      })

    }, {
      assert(true)
    })

  }

}
