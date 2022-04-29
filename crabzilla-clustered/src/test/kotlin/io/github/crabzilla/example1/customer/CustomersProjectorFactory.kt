package io.github.crabzilla.example1.customer

import io.github.crabzilla.EventProjector
import io.github.crabzilla.projection.EventProjectorProvider

class CustomersProjectorFactory : EventProjectorProvider {
  override fun create(): EventProjector {
    return CustomersEventProjector()
  }
}
