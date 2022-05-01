package io.github.crabzilla.command.internal

/**
 * A Snapshot is a state with a version
 */
internal data class Snapshot<S : Any>(val state: S, val version: Int)
