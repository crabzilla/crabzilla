package io.github.crabzilla.projection.verticle

import io.github.crabzilla.stack.projection.PgEventProjector

interface EventsProjectorProvider {
  fun create(): PgEventProjector
}
