package io.github.crabzilla.pgc.integration

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
