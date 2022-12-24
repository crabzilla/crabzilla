package io.github.crabzilla.stack.command

sealed class CommandServiceException(override val message: String, override val cause: Throwable? = null)
  : RuntimeException(message, cause) {
  class ConcurrencyException(message: String) : CommandServiceException(message)
  class BusinessException(message: String, cause: Throwable) : CommandServiceException(message, cause)
}
