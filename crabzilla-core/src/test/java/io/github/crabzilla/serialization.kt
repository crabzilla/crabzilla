package io.github.crabzilla

import io.github.crabzilla.SimpleSealed.SubSealedA
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule

// normal

interface Message

@Serializable
data class StringMessage(val message: String): Message

@Serializable
data class IntMessage(val number: Int): Message

// sealed

@Serializable
sealed class SimpleSealed {
  @Serializable
  data class SubSealedA(val s: String) : SimpleSealed()

  @Serializable
  data class SubSealedB(val i: Int) : SimpleSealed()
}



fun main() {

  val messageModule = SerializersModule { // 1
    polymorphic(Message::class) { // 2
      StringMessage::class with StringMessage.serializer() // 3
      IntMessage::class with IntMessage.serializer() // 4
    }
  }

  val json = Json(
    configuration = JsonConfiguration(useArrayPolymorphism = true),
    context = messageModule
  )

  val y = json.stringify(PolymorphicSerializer(Message::class), StringMessage("queue"))

  println(y)

// sealed

// will perform correct polymorphic serialization and deserialization:
  val x = Json.stringify(SimpleSealed.serializer(), SubSealedA("foo"))
  println(x)
// output will be
// {"type":"package.SimpleSealed.SubSealedA", "s":"foo"}
}
