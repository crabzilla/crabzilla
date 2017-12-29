package io.github.crabzilla.vertx.modules

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.helpers.VertxHelper
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import javax.inject.Singleton


// tag::module[]
@Module(includes = [WriteDbModule::class, ReadDbModule::class])
open class CrabzillaModule(val vertx: Vertx, val config: JsonObject) {

  init {
    configureVertx()
  }

  @Provides
  @Singleton
  fun vertx(): Vertx {
    return vertx
  }

  @Provides
  @Singleton
  fun config(): JsonObject {
    return config
  }

  fun configureVertx() {
    VertxHelper.initVertx(vertx)
  }

}
// end::module[]