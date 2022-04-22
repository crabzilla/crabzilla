package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
