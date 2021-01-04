package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable
import uk.ac.warwick.postroom.utils.OdtSerializer
import java.time.OffsetDateTime
import java.util.*

@Serializable
data class Courier(
    val id: String? = null,

    val name: String = "",

    val shortCode: String = "",

    @Serializable(with = OdtSerializer::class)
    val createdAt: OffsetDateTime? = null,

    val logoUrl: String
)

