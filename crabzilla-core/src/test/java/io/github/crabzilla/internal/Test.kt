package io.github.crabzilla.internal

import io.github.crabzilla.core.command.Command
import io.github.crabzilla.example1.ActivateCustomer
import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.Example1Fixture.example1Json
import java.time.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class Teste(@Contextual val ldt: LocalDateTime)

fun main() {
  val t = Teste(LocalDateTime.now())
  println(example1Json.encodeToString(t))
  val list = listOf(CreateCustomer("name1"), ActivateCustomer("ya"))
  val asJson = example1Json.encodeToString(list)
  println(asJson)
  println(example1Json.decodeFromString<List<Command>>(asJson))
}
