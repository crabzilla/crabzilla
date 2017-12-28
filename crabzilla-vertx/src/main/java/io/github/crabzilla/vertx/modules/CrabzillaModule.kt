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
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.entity.EntityCommandExecution
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

    Json.mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution::class.java,
            JacksonGenericCodec(Json.mapper, EntityCommandExecution::class.java))

    vertx.eventBus().registerDefaultCodec(EntityId::class.java,
            JacksonGenericCodec(Json.mapper, EntityId::class.java))

    vertx.eventBus().registerDefaultCodec(EntityCommand::class.java,
            JacksonGenericCodec(Json.mapper, EntityCommand::class.java))

    vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
            JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork::class.java,
            JacksonGenericCodec(Json.mapper, EntityUnitOfWork::class.java))

  }

}
// end::module[]