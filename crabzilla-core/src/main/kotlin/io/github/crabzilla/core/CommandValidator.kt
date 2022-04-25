package io.github.crabzilla.core

/**
 * To validate a command
 */
fun interface CommandValidator<C> {
  fun validate(command: C): List<String>
}
