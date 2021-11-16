package io.github.crabzilla.command.projector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
