package io.github.crabzilla.core

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<A : DomainState>(val state: A, val version: Int)
