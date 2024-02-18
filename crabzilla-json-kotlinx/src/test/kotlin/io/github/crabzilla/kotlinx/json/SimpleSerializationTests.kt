package io.github.crabzilla.kotlinx.json

import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.KClass

class SimpleSerializationTests {
  companion object {
    val json = Json { serializersModule = javaModule }
    val commandsMap =
      listOf(
        RegisterCustomer::class,
        ActivateCustomer::class,
        DeactivateCustomer::class,
        RegisterAndActivateCustomer::class,
      )
        .map { Pair(it.simpleName, it) }
    val eventsMap =
      listOf(CustomerRegistered::class, CustomerActivated::class, CustomerDeactivated::class)
        .map { Pair(it.simpleName, it) }
  }

  inline fun <reified T> encodeToString(value: T): String {
    return json.encodeToString(value)
  }

  inline fun <reified T : Any> decodeFromString(
    klass: KClass<T>,
    value: String,
  ): T {
    return json.decodeFromString(value)
  }

  @Test
  fun `command encode`() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson =
      """
      {"customerId":"${command.customerId}","name":"${command.name}"}
      """.trimIndent()
    assertThat(encodeToString(command)).isEqualTo(expectedJson)
  }

  @Test
  fun `command decode`() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson =
      """
      {"customerId":"${command.customerId}","name":"${command.name}"}
      """.trimIndent()
    assertThat(decodeFromString(RegisterCustomer::class, expectedJson)).isEqualTo(command)
  }
}
