package io.github.crabzilla.core.command

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.serializer

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<S: Any, C: Any, E: Any>(
  val stateSerDer: PolymorphicSerializer<S>,
  val commandSerDer: PolymorphicSerializer<C>,
  val eventSerDer: PolymorphicSerializer<E>,
  val eventHandler: EventHandler<S, E>,
  val commandHandlerFactory: () -> CommandHandler<S, C, E>,
  val commandValidator: CommandValidator<C>? = null
) {
  @OptIn(InternalSerializationApi::class)
  fun stateSerialName(): String = stateSerDer.baseClass.serializer().descriptor.serialName
}
