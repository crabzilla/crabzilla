package io.github.crabzilla.projection

import io.github.crabzilla.stack.PgEventProjector

interface EventsProjectorProvider {
  fun create(): PgEventProjector
}
