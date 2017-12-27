package io.github.crabzilla.core.entity

import io.github.crabzilla.core.DomainEvent

import java.io.Serializable

class SnapshotData(val version: Version, val events: List<DomainEvent>) : Serializable
