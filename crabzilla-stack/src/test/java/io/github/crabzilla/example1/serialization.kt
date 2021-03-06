package io.github.crabzilla.example1

import io.github.crabzilla.core.javaModule
import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.example1.payment.paymentModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * kotlinx.serialization
 */
@kotlinx.serialization.ExperimentalSerializationApi
val example1Module = SerializersModule {
  include(javaModule)
  include(customerModule)
  include(paymentModule)
}

val example1Json = Json { serializersModule = example1Module }
