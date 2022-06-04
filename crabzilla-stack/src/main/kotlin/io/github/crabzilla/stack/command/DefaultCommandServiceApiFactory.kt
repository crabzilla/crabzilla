package io.github.crabzilla.stack.command

import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.JsonObjectSerDer
import io.github.crabzilla.stack.command.internal.DefaultCommandServiceApi

class DefaultCommandServiceApiFactory(private val crabzilla: CrabzillaContext) : CommandServiceApiFactory {
  override fun <S : Any, C : Any, E : Any> commandService(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: CommandServiceOptions): CommandServiceApi<C> {
    return DefaultCommandServiceApi(crabzilla.vertx(), crabzilla.pgPool(), component, jsonObjectSerDer, options)
  }
}
