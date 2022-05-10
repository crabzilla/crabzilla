package io.github.crabzilla.stack.command

sealed class FeatureException(message: String) : Exception(message) {
  class ValidationException(errors: List<String>) : FeatureException(errors.toString())
  class ConcurrencyException(message: String) : FeatureException(message)
  class BusinessException(message: String) : FeatureException(message)
}
