package io.github.crabzilla

data class RangeOfEvents(val afterVersion: Version, val untilVersion: Version,
                         val events: List<Pair<String, DomainEvent>>)
