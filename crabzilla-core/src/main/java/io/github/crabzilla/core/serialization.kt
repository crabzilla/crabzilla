package io.github.crabzilla.core

import io.vertx.core.json.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

// kotlinx.serialization

// to serialize events http://www.smartjava.org/content/kotlin-arrow-typeclasses/

interface EventSerializer<E : Any> {
  fun toJson(e: E): Try<JsonObject>
}

interface EventDeserializer<E : Any> {
  fun fromJson(type: String, j: JsonObject): Try<E>
}

val AGGREGATE_ROOT_SERIALIZER = PolymorphicSerializer(AggregateRoot::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override fun serialize(encoder: Encoder, value: LocalDateTime) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE_TIME))
  }
  override fun deserialize(decoder: Decoder): LocalDateTime {
    return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
  }
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
  override fun serialize(encoder: Encoder, value: LocalDate) {
    encoder.encodeString(value.toString())
  }
  override fun deserialize(decoder: Decoder): LocalDate {
    return LocalDate.parse(decoder.decodeString())
  }
}

val javaModule = SerializersModule {
  contextual(LocalDateTime::class, LocalDateTimeSerializer)
  contextual(LocalDate::class, LocalDateSerializer)
}
