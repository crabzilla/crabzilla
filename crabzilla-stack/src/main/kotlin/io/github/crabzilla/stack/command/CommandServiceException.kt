package io.github.crabzilla.stack.command

sealed class CommandServiceException(override val message: String) : RuntimeException(message) {
  class ValidationException(errors: List<String>) : CommandServiceException(errors.toString())
  class ConcurrencyException(message: String) : CommandServiceException(message)
  class BusinessException(message: String) : CommandServiceException(message)
}
