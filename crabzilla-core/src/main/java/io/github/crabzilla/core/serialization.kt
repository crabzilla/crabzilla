package io.github.crabzilla.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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
@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
  override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder): UUID {
    return UUID.fromString(decoder.decodeString())
  }
  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }
}

// HACK: Wrap BigDecimal to extend Number, allowing us to serialize as an unquoted string.
private class BigDecimalNumber(val value: BigDecimal) : Number() {
  override fun toByte(): Byte = throw UnsupportedOperationException()
  override fun toChar(): Char = throw UnsupportedOperationException()
  override fun toDouble(): Double = throw UnsupportedOperationException()
  override fun toFloat(): Float = throw UnsupportedOperationException()
  override fun toInt(): Int = throw UnsupportedOperationException()
  override fun toLong(): Long = throw UnsupportedOperationException()
  override fun toShort(): Short = throw UnsupportedOperationException()
  override fun toString(): String = value.toEngineeringString()
}

@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

  override fun deserialize(decoder: Decoder): BigDecimal {
    val element = (decoder as JsonDecoder).decodeJsonElement() as JsonPrimitive
    return element.content.toBigDecimal()
  }

  override fun serialize(encoder: Encoder, value: BigDecimal) {
    val element = JsonPrimitive(BigDecimalNumber(value))
    (encoder as JsonEncoder).encodeJsonElement(element)
  }
}

@kotlinx.serialization.ExperimentalSerializationApi
val javaModule = SerializersModule {
  contextual(UUID::class, UUIDSerializer)
  contextual(BigDecimal::class, BigDecimalSerializer)
  contextual(LocalDateTime::class, LocalDateTimeSerializer)
  contextual(LocalDate::class, LocalDateSerializer)
}
