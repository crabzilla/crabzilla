package io.github.crabzilla.example1

import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.pgc.projector.EventsProjectorProvider

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomerEventsProjector
  }
}
