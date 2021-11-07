package io.github.crabzilla.postgres.projector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
