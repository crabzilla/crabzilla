package io.github.crabzilla.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class DomainEvent

@Serializable
abstract class Command

@Serializable
abstract class AggregateRoot {
  abstract fun id(): String
  var version = 0
  @Transient
  val changes: List<DomainEvent> = listOf()
  fun apply(vararg events: DomainEvent) {
    events.forEach { e ->
      applyEvent(e)
      changes.plus(e)
    }
  }
  fun reApply(vararg events: DomainEvent) {
    events.forEach { e ->
      applyEvent(e)
    }
  }
  abstract fun applyEvent(e: DomainEvent): Unit
}
