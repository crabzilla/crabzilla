package io.github.crabzilla.pgclient.projection

import org.slf4j.LoggerFactory
import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class EventsProjectorProviderFinder {
  companion object {
    val log = LoggerFactory.getLogger(EventsProjectorProviderFinder::class.java)
  }
  // provider by name
  fun create(providerName: String): EventsProjectorProvider {
    val loader: ServiceLoader<EventsProjectorProvider> =
      ServiceLoader.load(EventsProjectorProvider::class.java)
    val it: Iterator<EventsProjectorProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: EventsProjectorProvider = it.next()
      if (providerName == provider::class.java.name) {
        log.info("Found provider {}", provider)
        return provider
      }
    }
    throw ProviderNotFoundException("Provider $providerName not found")
  }
}
