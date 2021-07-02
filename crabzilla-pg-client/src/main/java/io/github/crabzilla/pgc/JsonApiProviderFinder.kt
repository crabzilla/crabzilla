package io.github.crabzilla.pgc

import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class JsonApiProviderFinder {

  // provider by name
  fun create(providerName: String): JsonApiProvider? {
    val loader: ServiceLoader<JsonApiProvider> =
      ServiceLoader.load(JsonApiProvider::class.java)
    val it: Iterator<JsonApiProvider> = loader.iterator()
    while (it.hasNext()) {
      val provider: JsonApiProvider = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("JsonApi provider $providerName not found")
  }
}
