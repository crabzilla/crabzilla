package io.github.crabzilla.stack

sealed class CommandException(message: String) : Exception(message) {
  class WriteConcurrencyException(message: String) : CommandException(message)
  class ValidationException(errors: List<String>) : CommandException(errors.toString())
}
