package io.github.crabzilla.framework

data class Snapshot<E : Entity>(val state: E, val version: Version)
