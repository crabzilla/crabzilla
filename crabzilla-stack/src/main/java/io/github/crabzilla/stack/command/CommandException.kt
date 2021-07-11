package io.github.crabzilla.stack.command

sealed class CommandException(message: String) : Exception(message) {
  class ValidationException(errors: List<String>) : CommandException(errors.toString())
  class OptimisticLockingException(message: String) : CommandException(message)
}
