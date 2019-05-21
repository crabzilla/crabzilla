package io.github.crabzilla

data class SnapshotData(val version: Version, val events: List<Pair<String, DomainEvent>>)
