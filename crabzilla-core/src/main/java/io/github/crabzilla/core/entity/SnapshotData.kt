package io.github.crabzilla.core.entity

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Version
import java.io.Serializable

class SnapshotData(val version: Version, val events: List<DomainEvent>) : Serializable
