package io.github.crabzilla.core.command

sealed class CommandException(message: String) : Exception(message) {
  class ValidationException(errors: List<String>) : CommandException(errors.toString())
  class LockingException(message: String) : CommandException(message)
//  class CommandHandlerException(e: Exception) : CommandException(e.message ?: e.toString())
}
