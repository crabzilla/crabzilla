package io.github.crabzilla.example1.customer

import io.github.crabzilla.projection.EventsProjector
import io.github.crabzilla.projection.EventsProjectorProvider

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomerEventsProjector
  }
}
