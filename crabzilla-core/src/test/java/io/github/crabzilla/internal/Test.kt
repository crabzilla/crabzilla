package io.github.crabzilla.internal

import io.github.crabzilla.core.Command
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.customerJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDateTime

@Serializable
data class AValueObject(val x: String, val y: Int)

@Serializable
data class Teste(@Contextual val ldt: LocalDateTime, val newProp1: String? = null)

fun main() {
  val t = Teste(LocalDateTime.now())
  val json = customerJson.encodeToString(t)
  println(json)
  println(customerJson.decodeFromString<Teste>(json))

  val list = listOf(
    CustomerCommand.RegisterCustomer(customerId = 1, name = "name1"),
    CustomerCommand.ActivateCustomer("ya")
  )
  val asJson = customerJson.encodeToString(list)
  println(asJson)
  println(customerJson.decodeFromString<List<Command>>(asJson))
}
