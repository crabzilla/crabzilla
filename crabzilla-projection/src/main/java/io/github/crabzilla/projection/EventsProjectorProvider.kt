package io.github.crabzilla.projection

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
