package io.github.crabzilla.core

/**
 * To handle commands
 */
abstract class CommandHandler<S, C, E>(private val applier: EventHandler<S, E>) {

  protected fun with(state: S): CommandSession<S, E> {
    return CommandSession(state, applier)
  }

  abstract fun handle(command: C, state: S): CommandSession<S, E>

  fun buildException(state: S, command: C): IllegalStateException {
    return IllegalStateException("Illegal transition. " +
      "state: ${state!!::class.java.name} command: ${command!!::class.java.name}")
  }

}
