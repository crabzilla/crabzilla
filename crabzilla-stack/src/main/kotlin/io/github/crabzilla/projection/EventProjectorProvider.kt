package io.github.crabzilla.projection

import io.github.crabzilla.EventProjector

interface EventProjectorProvider {
  fun create(): EventProjector
}
