package io.github.crabzilla.kotlinx

import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.core.json.JsonObject
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

internal class KotlinxJsonObjectSerDerTest {

  val json = Json { serializersModule = customerModule }
  private val serDer = KotlinxJsonObjectSerDer(json, customerComponent)

  @Test
  fun eventFromJson() {
    val id = UUID.randomUUID()
    val json = JsonObject()
      .put("type", "CustomerRegistered")
      .put("id", id.toString())
      .put("name", "c1")
    assertThat(serDer.eventFromJson(json.toString())).isEqualTo(CustomerRegistered(id, "c1"))
  }

  @Test
  fun eventToJsonObject() {
    val event = CustomerRegistered(UUID.randomUUID(), "c1")
    assertThat(serDer.eventToJsonObject(event).getString("type")).isEqualTo("CustomerRegistered")
  }

  @Test
  fun commandToJsonObject() {
    val command = CustomerCommand.RegisterCustomer(UUID.randomUUID(), "c1")
    assertThat(serDer.commandToJsonObject(command).getString("type")).isEqualTo("RegisterCustomer")
  }
}