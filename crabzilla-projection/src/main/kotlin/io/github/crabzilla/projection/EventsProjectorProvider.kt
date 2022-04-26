package io.github.crabzilla.projection

import io.github.crabzilla.stack.EventProjector

interface EventsProjectorProvider {
  fun create(): EventProjector
}
