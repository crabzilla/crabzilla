package io.github.crabzilla.core

/**
 * To handle commands
 */
abstract class CommandHandler<S, C, E>(private val applier: EventHandler<S, E>) {

  protected fun withNew(events: List<E>): FeatureSession<S, E> {
    return FeatureSession(events, applier)
  }
  protected fun with(state: S?): FeatureSession<S, E> {
    return FeatureSession(state!!, applier)
  }
  abstract fun handleCommand(command: C, state: S?): FeatureSession<S, E>
}
