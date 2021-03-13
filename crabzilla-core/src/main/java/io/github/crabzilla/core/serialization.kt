package io.github.crabzilla.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// kotlinx.serialization

val AGGREGATE_ROOT_SERIALIZER = PolymorphicSerializer(AggregateRoot::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)
val DOMAIN_EVENT_SERIALIZER = PolymorphicSerializer(DomainEvent::class)

@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override fun serialize(encoder: Encoder, value: LocalDateTime) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE_TIME))
  }
  override fun deserialize(decoder: Decoder): LocalDateTime {
    return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
  }
}

@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
  override fun serialize(encoder: Encoder, value: LocalDate) {
    encoder.encodeString(value.toString())
  }
  override fun deserialize(decoder: Decoder): LocalDate {
    return LocalDate.parse(decoder.decodeString())
  }
}

@kotlinx.serialization.ExperimentalSerializationApi
val javaModule = SerializersModule {
  contextual(LocalDateTime::class, LocalDateTimeSerializer)
  contextual(LocalDate::class, LocalDateSerializer)
}
