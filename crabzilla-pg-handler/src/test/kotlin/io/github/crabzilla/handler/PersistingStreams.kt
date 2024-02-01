package io.github.crabzilla.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.example1.customer.Customer
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistingStreams {
  @Test
  @DisplayName("a TODO ")
  @Disabled
  fun s1(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    // TODO WIP to migrate a stream to another stream leveraging previous state
    val customer = Customer.Active(UUID.randomUUID(), "c1", "cust#1")
    val objectMapper = ObjectMapper().writerWithDefaultPrettyPrinter()
    println(customer::class.java.simpleName)
    println(objectMapper.writeValueAsString(customer))
    // TODO end
  }
}
