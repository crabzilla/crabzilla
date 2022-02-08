package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command

/**
 * To validate a command
 */
fun interface CommandValidator<C : Command> {
  fun validate(command: C): List<String>
}
