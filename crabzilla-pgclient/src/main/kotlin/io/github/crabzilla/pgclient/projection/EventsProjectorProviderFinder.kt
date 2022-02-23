package io.github.crabzilla.pgclient.projection

import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class EventsProjectorProviderFinder {
  // provider by name
  fun create(providerName: String): EventsProjectorProvider {
    val loader: ServiceLoader<EventsProjectorProvider> =
      ServiceLoader.load(EventsProjectorProvider::class.java)
    val it: Iterator<EventsProjectorProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: EventsProjectorProvider = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("Provider $providerName not found")
  }
}
