package io.github.crabzilla.stack

/**
 * A Snapshot is a state with a version
 */
data class Snapshot<S : Any>(val state: S, val version: Int)
