package io.github.crabzilla.example1.customer

import io.github.crabzilla.engine.projector.EventsProjector
import io.github.crabzilla.engine.projector.EventsProjectorProvider

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomerEventsProjector
  }
}
