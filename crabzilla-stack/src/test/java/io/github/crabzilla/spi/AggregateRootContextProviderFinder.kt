package io.github.crabzilla.spi

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import java.nio.file.ProviderNotFoundException
import java.util.ServiceLoader

class AggregateRootContextProviderFinder<A : DomainState, C : Command, E : DomainEvent> {

  // provider by name
  fun create(providerName: String): AggregateRootContextProvider<A, C, E>? {
    val loader: ServiceLoader<AggregateRootContextProvider<A, C, E>> =
      ServiceLoader.load(AggregateRootContextProvider::class.java)
        as ServiceLoader<AggregateRootContextProvider<A, C, E>>
    val it: Iterator<AggregateRootContextProvider<A, C, E>> = loader.iterator()
    while (it.hasNext()) {
      val provider: AggregateRootContextProvider<A, C, E> = it.next()
      if (providerName == provider::class.java.name) {
        return provider
      }
    }
    throw ProviderNotFoundException("JsonContext provider $providerName not found")
  }
}
