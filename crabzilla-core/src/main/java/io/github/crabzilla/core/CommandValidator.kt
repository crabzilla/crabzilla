package io.github.crabzilla.core

/**
 * To validate a command
 */
fun interface CommandValidator<C : Command> {
  fun validate(command: C): List<String>
}
