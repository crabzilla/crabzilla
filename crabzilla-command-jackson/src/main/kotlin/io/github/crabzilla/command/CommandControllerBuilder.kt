package io.github.crabzilla.command

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.core.command.CommandControllerConfig
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool

class CommandControllerBuilder(private val vertx: Vertx, private val pgPool: PgPool) {

  fun <S : Any, C : Any, E : Any> build(
    objectMapper: ObjectMapper,
    config: CommandControllerConfig<S, C, E>,
    eventsProjector: EventsProjector? = null // use this just to solve uniqueness validation
  ): CommandController<S, C, E> {
    return CommandController(vertx, pgPool, objectMapper, config, eventsProjector)
  }
}
