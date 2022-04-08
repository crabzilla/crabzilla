package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.pgclient.EventsProjector
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class CommandControllerBuilder(private val vertx: Vertx, private val pgPool: PgPool) {

  fun <S : Any, C : Any, E : Any> build(
    serializationModule: SerializersModule,
    config: CommandControllerConfig<S, C, E>,
    eventsProjector: EventsProjector? = null,
  ): CommandController<S, C, E> {
    val json = Json { serializersModule = serializationModule }
    return CommandController(vertx, pgPool, json, config, eventsProjector)
  }

}
