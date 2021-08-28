package io.github.crabzilla.engine

import io.github.crabzilla.serder.SerDer

interface JsonContext {
  fun serder(): SerDer
}
