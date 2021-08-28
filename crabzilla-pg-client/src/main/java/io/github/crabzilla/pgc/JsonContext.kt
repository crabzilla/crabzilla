package io.github.crabzilla.pgc

import io.github.crabzilla.core.serder.SerDer

interface JsonContext {
  fun serder(): SerDer
}
