package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.vertx.configHandler
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class PgClientModuleIT {

  @Test
  fun load(vertx: Vertx, tc: VertxTestContext) {

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "example1.env"))

    configHandler(vertx, envOptions, { config ->

      vertx.executeBlocking<Any>({ future ->

        val component = DaggerPgClientComponent.builder()
          .pgClientModule(PgClientModule(vertx, config))
          .build()

        assertNotNull(component.readDb())
        assertNotNull(component.writeDb())

        component.readDb().query("select 1", { event -> println("ok read? : " + event.succeeded()) })

        component.writeDb().query("select 1", { event -> println("ok write? : " + event.succeeded()) })

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
