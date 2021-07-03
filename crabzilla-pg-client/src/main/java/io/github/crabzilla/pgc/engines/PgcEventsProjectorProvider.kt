package io.github.crabzilla.pgc.engines

interface PgcEventsProjectorProvider {
  fun create(): PgcEventsProjectorApi
}
