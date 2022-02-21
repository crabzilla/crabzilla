package io.github.crabzilla.core.command

/**
 * To validate a command
 */
fun interface CommandValidator<C> {
  fun validate(command: C): List<String>
}
