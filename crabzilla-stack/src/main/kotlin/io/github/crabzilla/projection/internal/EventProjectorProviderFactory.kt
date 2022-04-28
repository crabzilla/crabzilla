package io.github.crabzilla.projection.internal

import io.github.crabzilla.projection.EventsProjectorProvider
import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

internal class EventProjectorProviderFactory {
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
