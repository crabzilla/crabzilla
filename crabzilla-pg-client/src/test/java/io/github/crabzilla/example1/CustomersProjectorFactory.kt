package io.github.crabzilla.example1

import io.github.crabzilla.pgc.integration.EventsProjector
import io.github.crabzilla.pgc.integration.EventsProjectorProvider

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomerEventsProjector
  }
}
