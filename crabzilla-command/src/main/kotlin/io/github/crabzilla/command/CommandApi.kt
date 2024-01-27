package io.github.crabzilla.command

import CrabzillaContext
import EventMetadata
import EventProjector
import JsonObjectSerDer
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import kotlin.reflect.KClass

interface CommandComponent<C : Any> {
  fun handle(
    stateId: String,
    command: C,
    versionPredicate: ((Int) -> Boolean)? = null,
  ): Future<EventMetadata>

  fun handle(
    conn: SqlConnection,
    stateId: String,
    command: C,
    versionPredicate: ((Int) -> Boolean)? = null,
  ): Future<EventMetadata>
}

data class CommandComponentConfig<S : Any, C : Any, E : Any>(
  val stateClass: KClass<S>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  val eventSerDer: JsonObjectSerDer<E>,
  val eventHandler: (S, E) -> S,
  val commandHandler: (S, C) -> List<E>,
  val initialState: S,
  val eventProjector: EventProjector? = null,
  val eventBusTopic: String? = null,
  val eventStreamSize: Int = 1000,
  val persistCommands: Boolean = true,
)

object CommandComponentFactory {
  fun <S : Any, C : Any, E : Any> create(
    context: CrabzillaContext,
    config: CommandComponentConfig<S, C, E>,
  ): CommandComponent<C> {
    return DefaultCommandComponent(context, config)
  }
}

sealed class CommandComponentException(override val message: String, override val cause: Throwable? = null) :
  RuntimeException(message, cause) {
  class ConcurrencyException(message: String) : CommandComponentException(message)

  class BusinessException(message: String, cause: Throwable) : CommandComponentException(message, cause)
}
