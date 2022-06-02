package io.github.crabzilla.kotlinx

import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.stack.CrabzillaVertxContext
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.command.internal.CommandService
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

fun <S: Any, C: Any, E: Any>CrabzillaVertxContext.kotlinxCommandService(
  component: FeatureComponent<S, C, E>,
  serDerModule: SerializersModule,
  options: CommandServiceOptions = CommandServiceOptions()
): CommandServiceApi<C> {
  val json = Json { serializersModule = serDerModule }
  val serDer = KotlinxJsonObjectSerDer(json, component)
  return CommandService(this.vertx(), this.pgPool(), component, serDer, options)
}
