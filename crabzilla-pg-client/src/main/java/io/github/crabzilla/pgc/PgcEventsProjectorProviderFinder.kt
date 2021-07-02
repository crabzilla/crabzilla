package io.github.crabzilla.pgc

import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class PgcEventsProjectorProviderFinder {
  // provider by name
  fun create(providerName: String): PgcEventsProjectorProvider? {
    val loader: ServiceLoader<PgcEventsProjectorProvider> =
      ServiceLoader.load(PgcEventsProjectorProvider::class.java)
    val it: Iterator<PgcEventsProjectorProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: PgcEventsProjectorProvider = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("Provider $providerName not found")
  }
}
