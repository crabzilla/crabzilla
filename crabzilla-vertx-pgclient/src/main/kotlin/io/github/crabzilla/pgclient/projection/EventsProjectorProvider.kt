package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.pgclient.EventsProjector

interface EventsProjectorProvider {
  fun create(): EventsProjector
}
