package io.github.crabzilla.pgc

import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class JsonContextProviderFinder {

  // provider by name
  fun create(providerName: String): JsonContextProvider? {
    val loader: ServiceLoader<JsonContextProvider> =
      ServiceLoader.load(JsonContextProvider::class.java)
    val it: Iterator<JsonContextProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: JsonContextProvider = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("JsonContext provider $providerName not found")
  }
}
