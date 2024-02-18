package io.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.crabzilla.example1.customer.CustomerEvent
import io.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class JacksonEventSerDerTest {
  private val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
  private val serDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class)

  @Test
  fun eventFromJson() {
    val id = UUID.randomUUID()
    val json =
      JsonObject()
        .put("type", "CustomerRegistered")
        .put("id", id)
        .put("name", "c1")
    assertThat(serDer.fromJson(json)).isEqualTo(CustomerRegistered(id, "c1"))
  }

  @Test
  fun eventToJsonObject() {
    val event = CustomerRegistered(UUID.randomUUID(), "c1")
    assertThat(serDer.toJson(event).getString("type")).isEqualTo("CustomerRegistered")
  }
}
