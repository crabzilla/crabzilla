package io.github.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.customerComponent
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class JacksonJsonObjectSerDerTest {

  private val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
  private val serDer = JacksonJsonObjectSerDer(json, customerComponent)

  @Test
  fun eventFromJson() {
    val id = UUID.randomUUID()
    val json = JsonObject()
      .put("type", "CustomerRegistered")
      .put("id", id)
      .put("name", "c1")
    assertThat(serDer.eventFromJson(json)).isEqualTo(CustomerRegistered(id, "c1"))
  }

  @Test
  fun eventToJsonObject() {
    val event = CustomerRegistered(UUID.randomUUID(), "c1")
    assertThat(serDer.eventToJson(event).getString("type")).isEqualTo("CustomerRegistered")
  }

  @Test
  fun commandToJsonObject() {
    val command = RegisterCustomer(UUID.randomUUID(), "c1")
    assertThat(serDer.commandToJson(command).getString("type")).isEqualTo("RegisterCustomer")
  }

  @Test
  fun commandFromJson() {
    val id = UUID.randomUUID()
    val json = JsonObject()
      .put("type", "RegisterCustomer")
      .put("customerId", id.toString())
      .put("name", "c1")
    assertThat(serDer.commandFromJson(json)).isEqualTo(RegisterCustomer(id, "c1"))
  }

}
