package io.github.crabzilla.stack.command

sealed class CommandServiceException(override val cause: Throwable) : RuntimeException(cause) {
  class ConcurrencyException(cause: Throwable) : CommandServiceException(cause)
  class BusinessException(cause: Throwable) : CommandServiceException(cause)
}
