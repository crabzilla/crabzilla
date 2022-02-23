package io.github.crabzilla.example1.customer

import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.projection.EventsProjectorProvider

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomersEventsProjector("customers")
  }
}
