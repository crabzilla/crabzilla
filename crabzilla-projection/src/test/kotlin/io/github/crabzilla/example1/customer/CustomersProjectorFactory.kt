package io.github.crabzilla.example1.customer

import io.github.crabzilla.projection.verticle.EventsProjectorProvider
import io.github.crabzilla.stack.EventsProjector

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventsProjector {
    return CustomersEventsProjector("customers")
  }
}
