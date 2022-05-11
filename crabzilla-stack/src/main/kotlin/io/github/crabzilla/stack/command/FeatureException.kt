package io.github.crabzilla.stack.command

sealed class FeatureException(override val message: String) : RuntimeException(message) {
  class ValidationException(errors: List<String>) : FeatureException(errors.toString())
  class ConcurrencyException(message: String) : FeatureException(message)
  class BusinessException(message: String) : FeatureException(message)
}
