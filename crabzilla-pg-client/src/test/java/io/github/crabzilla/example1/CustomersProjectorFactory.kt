package io.github.crabzilla.example1

import io.github.crabzilla.pgc.engines.PgcEventsProjectorApi
import io.github.crabzilla.pgc.engines.PgcEventsProjectorProvider

class CustomersProjectorFactory : PgcEventsProjectorProvider {
  override fun create(): PgcEventsProjectorApi {
    return CustomerEventsProjector
  }
}
