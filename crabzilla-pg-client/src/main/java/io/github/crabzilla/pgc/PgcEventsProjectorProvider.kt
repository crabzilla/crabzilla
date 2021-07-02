package io.github.crabzilla.pgc

interface PgcEventsProjectorProvider {
  fun create(): PgcEventsProjectorApi
}
