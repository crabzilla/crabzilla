package io.github.crabzilla.pgclient

import io.github.crabzilla.vertx.CrabzillaMainModule
import io.github.crabzilla.vertx.configHandler
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("PgClientModule")
@ExtendWith(VertxExtension::class)
class PgClientModuleIT {

  @Test
  fun load(vertx: Vertx, tc: VertxTestContext) {

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    configHandler(vertx, envOptions, { config ->

      vertx.executeBlocking<Any>({ future ->

        val component = DaggerPgClientTestComponent.builder()
          .crabzillaMainModule(CrabzillaMainModule(vertx, config))
          .pgClientModule(PgClientModule())
          .build()

        assertNotNull(component.readDb())
        assertNotNull(component.writeDb())

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
