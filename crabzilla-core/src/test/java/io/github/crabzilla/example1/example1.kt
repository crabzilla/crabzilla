package io.github.crabzilla.example1

import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.framework.EVENT_SERIALIZER
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val example1Json = Json(
  configuration = JsonConfiguration(useArrayPolymorphism = false),
  context = customerModule
)

fun main() {

  val y = example1Json.stringify(EVENT_SERIALIZER.list, listOf(CustomerCreated(1, "c1"), CustomerActivated("yeah")))

  println(y)

}
