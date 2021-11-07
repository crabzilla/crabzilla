package io.github.crabzilla.engine.projector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
