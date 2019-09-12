package io.github.crabzilla.internal

import io.github.crabzilla.framework.Version
import io.github.crabzilla.framework.DomainEvent

data class RangeOfEvents(val afterVersion: Version,
                         val untilVersion: Version,
                         val events: List<Pair<String, DomainEvent>>)
