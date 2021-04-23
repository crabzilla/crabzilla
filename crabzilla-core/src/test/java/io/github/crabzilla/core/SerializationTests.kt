package io.github.crabzilla.core

import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.customerJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("A StatefulSession")
class SerializationTests {

  @Test // TODO assertions
  fun test() {
    val t = Teste(LocalDateTime.now())
    val json = customerJson.encodeToString(t)
    println(json)
    println(customerJson.decodeFromString<Teste>(json))

    val list = listOf(
      CustomerCommand.RegisterCustomer(customerId = UUID.randomUUID(), name = "name1"),
      CustomerCommand.ActivateCustomer("ya")
    )
    val asJson = customerJson.encodeToString(list)
    println(asJson)
    println(customerJson.decodeFromString<List<Command>>(asJson))
  }
}

@Serializable
data class AValueObject(val x: String, val y: Int)

@Serializable
data class Teste(@Contextual val ldt: LocalDateTime, val newProp1: String? = null)
