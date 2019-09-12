package io.github.crabzilla.internal

import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.Version

data class RangeOfEvents(val afterVersion: Version,
                         val untilVersion: Version,
                         val events: List<Pair<String, DomainEvent>>)
