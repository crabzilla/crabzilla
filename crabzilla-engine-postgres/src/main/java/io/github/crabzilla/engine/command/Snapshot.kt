package io.github.crabzilla.engine.command

import io.github.crabzilla.core.State

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<S : State>(val state: S, val version: Int)