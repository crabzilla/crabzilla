package io.github.crabzilla.kotlinx

import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.core.json.JsonObject
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class KotlinxCommandSerDerTest {
  private val json = Json { serializersModule = customerModule }
  private val serDer = KotlinxSerDer(json, clazz = CustomerCommand::class)

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
