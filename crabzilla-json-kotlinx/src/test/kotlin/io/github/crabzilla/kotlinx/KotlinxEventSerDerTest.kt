package io.github.crabzilla.kotlinx

import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.core.json.JsonObject
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class KotlinxEventSerDerTest {
  private val json = Json { serializersModule = customerModule }
  private val serDer = KotlinxSerDer(json, clazz = CustomerEvent::class)

  @Test
  fun eventFromJson() {
    val id = UUID.randomUUID()
    val json =
      JsonObject()
        .put("type", "CustomerRegistered")
        .put("id", id.toString())
        .put("name", "c1")
    assertThat(serDer.fromJson(json)).isEqualTo(CustomerRegistered(id, "c1"))
  }

  @Test
  fun eventToJsonObject() {
    val event = CustomerRegistered(UUID.randomUUID(), "c1")
    assertThat(serDer.toJson(event).getString("type")).isEqualTo("CustomerRegistered")
  }
}
