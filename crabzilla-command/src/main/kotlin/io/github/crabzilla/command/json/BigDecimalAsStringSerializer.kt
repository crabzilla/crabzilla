package io.github.crabzilla.command.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = BigDecimal::class)
object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
  override val descriptor = PrimitiveSerialDescriptor("decimal", PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder): BigDecimal {
    return BigDecimal(decoder.decodeString())
  }
  override fun serialize(encoder: Encoder, value: BigDecimal) {
    encoder.encodeString(value.toPlainString())
  }
}
