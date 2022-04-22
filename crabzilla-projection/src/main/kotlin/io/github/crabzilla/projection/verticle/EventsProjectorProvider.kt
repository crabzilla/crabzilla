package io.github.crabzilla.projection.verticle

import io.github.crabzilla.stack.EventsProjector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
