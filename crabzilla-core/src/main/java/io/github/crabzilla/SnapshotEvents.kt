package io.github.crabzilla

data class SnapshotEvents(val version: Version, val events: List<Pair<String, DomainEvent>>)
