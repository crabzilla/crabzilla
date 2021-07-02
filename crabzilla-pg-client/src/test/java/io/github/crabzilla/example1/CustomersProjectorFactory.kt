package io.github.crabzilla.example1

import io.github.crabzilla.pgc.PgcEventsProjectorApi
import io.github.crabzilla.pgc.PgcEventsProjectorProvider

class CustomersProjectorFactory : PgcEventsProjectorProvider {
  override fun create(): PgcEventsProjectorApi {
    return CustomerEventsProjector
  }
}
