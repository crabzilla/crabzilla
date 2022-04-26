package io.github.crabzilla.example1.customer

import io.github.crabzilla.projection.EventsProjectorProvider
import io.github.crabzilla.stack.EventProjector

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): EventProjector {
    return CustomersEventProjector()
  }
}
