package io.github.crabzilla.command.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
