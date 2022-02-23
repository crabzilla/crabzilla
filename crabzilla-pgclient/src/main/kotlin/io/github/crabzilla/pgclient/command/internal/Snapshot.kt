package io.github.crabzilla.pgclient.command.internal

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<S : Any>(val state: S, val version: Int)
