package io.github.crabzilla.example1.customer

import io.github.crabzilla.projection.EventsProjectorProvider
import io.github.crabzilla.stack.PgEventProjector

class CustomersProjectorFactory : EventsProjectorProvider {
  override fun create(): PgEventProjector {
    return CustomersPgEventProjector()
  }
}
