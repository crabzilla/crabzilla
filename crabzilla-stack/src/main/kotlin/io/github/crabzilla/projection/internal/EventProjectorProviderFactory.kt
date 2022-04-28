package io.github.crabzilla.projection.internal

import io.github.crabzilla.projection.EventProjectorProvider
import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

internal class EventProjectorProviderFactory {
  // provider by name
  fun create(providerName: String): EventProjectorProvider {
    val loader: ServiceLoader<EventProjectorProvider> =
      ServiceLoader.load(EventProjectorProvider::class.java)
    val it: Iterator<EventProjectorProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: EventProjectorProvider = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("Provider $providerName not found")
  }
}
