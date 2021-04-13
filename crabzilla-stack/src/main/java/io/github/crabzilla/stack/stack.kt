package io.github.crabzilla.stack

/**
 * An exception informing an concurrency violation
 */
class OptimisticConcurrencyConflict(message: String) : IllegalStateException(message)

data class ValidationException(val errors: List<String>) : RuntimeException(errors.toString())
