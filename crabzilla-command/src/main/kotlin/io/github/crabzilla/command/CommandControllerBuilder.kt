package io.github.crabzilla.command

import io.github.crabzilla.command.json.javaModule
import io.github.crabzilla.core.command.CommandControllerConfig
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class CommandControllerBuilder(private val vertx: Vertx, private val pgPool: PgPool) {

  fun <S : Any, C : Any, E : Any> build(
    serializationModule: SerializersModule,
    config: CommandControllerConfig<S, C, E>,
    eventsProjector: EventsProjector? = null // use this just to solve uniqueness validation
  ): CommandController<S, C, E> {
    val module = SerializersModule {
      include(javaModule)
      include(serializationModule)
    }
    val json = Json { serializersModule = module }
    return CommandController(vertx, pgPool, json, config, eventsProjector)
  }
}
