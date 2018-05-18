package io.github.crabzilla.vertx.modules

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.initVertx
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import javax.inject.Singleton

@Module(includes = [WriteDbModule::class, ReadDbModule::class])
open class CrabzillaModule(val vertx: Vertx, val config: JsonObject) {

  init {

    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
    LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

    initVertx(vertx)

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

}
