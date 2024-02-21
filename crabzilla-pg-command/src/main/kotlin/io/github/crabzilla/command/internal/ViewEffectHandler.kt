package io.github.crabzilla.command.internal

import io.github.crabzilla.command.CommandHandlerResult
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

interface ViewEffectHandler<S : Any, E : Any> {
  fun handle(commandHandlerResult: CommandHandlerResult<S, E>): Future<JsonObject?>
}
