package io.github.crabzilla.command

import com.github.benmanes.caffeine.cache.Cache
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.ViewTrigger
import io.github.crabzilla.stream.StreamSnapshot

data class CommandHandlerConfig<S : Any, C : Any, E : Any>(
  val initialState: S,
  val evolveFunction: (S, E) -> S,
  val decideFunction: (S, C) -> List<E>,
  val injectFunction: ((S) -> S)? = null,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  val viewEffect: ViewEffect<S, E>? = null,
  val viewTrigger: ViewTrigger? = null,
  val persistEvents: Boolean? = true,
  val persistCommands: Boolean? = commandSerDer != null,
  val notifyPostgres: Boolean = true,
  val snapshotCache: Cache<Int, StreamSnapshot<S>>? = null,
)
