package io.github.crabzilla.pgc.projector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
