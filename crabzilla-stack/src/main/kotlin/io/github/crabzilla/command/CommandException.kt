package io.github.crabzilla.command

sealed class CommandException(message: String) : Exception(message) {
  class ValidationException(errors: List<String>) : CommandException(errors.toString())
  class LockingException(message: String) : CommandException(message)
}