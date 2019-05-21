package io.github.crabzilla

data class Snapshot<E : Entity>(val state: E, val version: Version)
