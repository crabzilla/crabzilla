package io.github.crabzilla.stack

import java.util.UUID

/**
 * An exception informing an concurrency violation
 */
class OptimisticConcurrencyConflict(message: String) : IllegalStateException(message)

data class ValidationException(val errors: List<String>) : RuntimeException(errors.toString())

/**
 * The client must knows how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: Int,
  val id: UUID = UUID.randomUUID(),
  val causationId: UUID = id,
  val correlationID: UUID = id
)
