package io.github.crabzilla

data class Snapshot<A : Entity>(val instance: A, val version: Version)
