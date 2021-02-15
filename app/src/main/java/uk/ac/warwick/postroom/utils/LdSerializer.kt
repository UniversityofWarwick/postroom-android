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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Serializer(forClass = LdSerializer::class)
object LdSerializer : KSerializer<LocalDate?> {

    private val delegate = String.serializer().nullable

    override fun serialize(encoder: Encoder, value: LocalDate?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE))
        }
    }

    override fun deserialize(decoder: Decoder): LocalDate? {
        val input = delegate.deserialize(decoder)
        return listOf(input).mapNotNull {
            LocalDate.parse(it)
        }.firstOrNull()
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate?", PrimitiveKind.STRING)
}