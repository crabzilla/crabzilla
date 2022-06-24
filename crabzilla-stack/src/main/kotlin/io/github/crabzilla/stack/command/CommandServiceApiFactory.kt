package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.JsonObjectSerDer

interface CommandServiceApiFactory {

  fun <S : Any, C : Any, E : Any> commandService(
    component: CommandServiceConfig<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: CommandServiceOptions = CommandServiceOptions(),
  ): CommandServiceApi<C>

}
