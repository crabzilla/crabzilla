package io.github.crabzilla.stack.command.internal

internal data class Snapshot<S : Any>(val state: S, val version: Int, val causationId: String?, val correlationId: String?)
