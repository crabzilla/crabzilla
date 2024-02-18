package io.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.crabzilla.example1.customer.CustomerCommand
import io.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class JacksonCommandSerDerTest {
  private val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
  private val serDer = JacksonJsonObjectSerDer(json, clazz = CustomerCommand::class)

  @Test
  fun commandToJsonObject() {
    val command = RegisterCustomer(UUID.randomUUID(), "c1")
    assertThat(serDer.toJson(command).getString("type")).isEqualTo("RegisterCustomer")
  }

  @Test
  fun commandFromJson() {
    val id = UUID.randomUUID()
    val json =
      JsonObject()
        .put("type", "RegisterCustomer")
        .put("customerId", id.toString())
        .put("name", "c1")
    assertThat(serDer.fromJson(json)).isEqualTo(RegisterCustomer(id, "c1"))
  }
}
