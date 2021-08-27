package io.github.crabzilla.example1

import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.example1.payment.paymentModule
import io.github.crabzilla.pgc.JsonContext
import io.github.crabzilla.pgc.JsonContextProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return Example1JsonContext()
  }
  class Example1JsonContext : JsonContext {
    @kotlinx.serialization.ExperimentalSerializationApi
    val example1Module = SerializersModule {
      include(customerModule)
      include(paymentModule)
    }
    val example1Json = Json { serializersModule = example1Module }
    override fun json(): Json {
      return example1Json
    }
  }
}
