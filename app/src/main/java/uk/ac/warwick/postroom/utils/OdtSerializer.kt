package uk.ac.warwick.postroom.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Serializer(forClass = OdtSerializer::class)
object OdtSerializer : KSerializer<OffsetDateTime?> {

    private val delegate = String.serializer().nullable

    override fun serialize(encoder: Encoder, value: OffsetDateTime?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE_TIME))
        }
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime? {
        val input = delegate.deserialize(decoder)
        return listOf(input).mapNotNull {
            OffsetDateTime.parse(it)
        }.firstOrNull()
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OffsetDateTime?", PrimitiveKind.STRING)
}