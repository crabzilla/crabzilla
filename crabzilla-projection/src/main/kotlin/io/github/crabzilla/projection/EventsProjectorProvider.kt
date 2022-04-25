package io.github.crabzilla.projection

import io.github.crabzilla.stack.projection.PgEventProjector

interface EventsProjectorProvider {
  fun create(): PgEventProjector
}
