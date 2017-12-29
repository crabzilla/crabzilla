package io.github.crabzilla.vertx.modules

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import dagger.Module
import dagger.Provides
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.vertx.EntityCommandExecution
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.helpers.VertxHelper
import io.vertx.core.Vertx
import io.vertx.core.json.Json
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