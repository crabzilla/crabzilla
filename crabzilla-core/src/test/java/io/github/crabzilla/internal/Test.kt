package io.github.crabzilla.internal

import io.github.crabzilla.example1.Example1Fixture.example1Json
import java.time.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class Teste(@Contextual val ldt: LocalDateTime, val newProp1: String? = null)

fun main() {
  val t = Teste(LocalDateTime.now())
  val json = example1Json.encodeToString(t)
  println(json)
  println(example1Json.decodeFromString<Teste>(json))

//  val list = listOf(CreateCustomer("name1"), ActivateCustomer("ya"))
//  val asJson = example1Json.encodeToString(list)
//  println(asJson)
//  println(example1Json.decodeFromString<List<Command>>(asJson))
}
